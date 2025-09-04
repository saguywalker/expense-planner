use axum::{
    extract::State,
    routing::{get, post},
    Json, Router,
};
use rusqlite::{Connection, Result};
use serde::{Deserialize, Serialize};
use std::sync::Arc;
use tokio::sync::Mutex;
use tower_http::cors::CorsLayer;

#[derive(Debug, Serialize, Deserialize)]
struct Expense {
    id: Option<i64>,
    description: String,
    amount: f64,
    paid_by: String,
    split_type: String,
    custom_splits: Option<String>,
}

struct AppState {
    db: Mutex<Connection>,
}

#[tokio::main]
async fn main() {
    // Initialize database
    let conn = Connection::open("expenses.db").expect("Failed to open database");

    // Create expenses table
    conn.execute(
        "CREATE TABLE IF NOT EXISTS expenses (
            id INTEGER PRIMARY KEY,
            description TEXT NOT NULL,
            amount REAL NOT NULL,
            paid_by TEXT NOT NULL,
            split_type TEXT NOT NULL,
            custom_splits TEXT
        )",
        [],
    )
    .expect("Failed to create table");

    let shared_state = Arc::new(AppState {
        db: Mutex::new(conn),
    });

    let cors = CorsLayer::permissive();

    let app = Router::new()
        .route("/expenses", get(get_expenses))
        .route("/expenses", post(add_expense))
        .layer(cors)
        .with_state(shared_state);

    println!("Server running on http://localhost:8080");

    let listener = tokio::net::TcpListener::bind("0.0.0.0:8080").await.unwrap();
    axum::serve(listener, app).await.unwrap();
}

async fn get_expenses(State(state): State<Arc<AppState>>) -> Json<Vec<Expense>> {
    let conn = state.db.lock().await;
    let mut stmt = conn
        .prepare("SELECT id, description, amount, paid_by, split_type, custom_splits FROM expenses")
        .expect("Failed to prepare statement");

    let expenses = stmt
        .query_map([], |row| {
            Ok(Expense {
                id: Some(row.get(0)?),
                description: row.get(1)?,
                amount: row.get(2)?,
                paid_by: row.get(3)?,
                split_type: row.get(4)?,
                custom_splits: row.get(5)?,
            })
        })
        .expect("Failed to execute query")
        .filter_map(Result::ok)
        .collect::<Vec<_>>();

    Json(expenses)
}

async fn add_expense(
    State(state): State<Arc<AppState>>,
    Json(expense): Json<Expense>,
) -> Json<Expense> {
    let conn = state.db.lock().await;

    conn.execute(
        "INSERT INTO expenses (description, amount, paid_by, split_type, custom_splits)
         VALUES (?1, ?2, ?3, ?4, ?5)",
        (
            &expense.description,
            expense.amount,
            &expense.paid_by,
            &expense.split_type,
            expense.custom_splits.as_deref(),
        ),
    )
    .expect("Failed to insert expense");

    let id = conn.last_insert_rowid();

    Json(Expense {
        id: Some(id),
        ..expense
    })
}
