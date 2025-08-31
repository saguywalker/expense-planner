//package myorg
//
//class Temp:
//  println("hello")
//
//import cats.effect.IO
//import org.scalajs.dom
//import tyrian.Html.*
//import tyrian.*
//import tyrian.cmds.Logger
//
//import scala.scalajs.js.annotation.*
//import scala.scalajs.js
//import scala.concurrent.ExecutionContext.Implicits.global
//import scala.util.{Failure, Success}
//
//// Facade for Firebase Firestore functions
//@js.native
//@JSImport("firebase/app", "initializeApp")
//object initializeApp extends js.Function1[js.Object, js.Dynamic] {
//  def apply(config: js.Object): js.Dynamic = js.native
//}
//
//@js.native
//@JSImport("firebase/firestore", "getFirestore")
//object getFirestore extends js.Function1[js.Dynamic, js.Dynamic] {
//  def apply(app: js.Dynamic): js.Dynamic = js.native
//}
//
//@js.native
//@JSImport("firebase/firestore", "collection")
//object collection extends js.Function2[js.Dynamic, String, js.Dynamic] {
//  def apply(db: js.Dynamic, path: String): js.Dynamic = js.native
//}
//
//@js.native
//@JSImport("firebase/firestore", "onSnapshot")
//object onSnapshot extends js.Function2[js.Dynamic, js.Function1[js.Dynamic, Unit], Unit] {
//  def apply(query: js.Dynamic, callback: js.Function1[js.Dynamic, Unit]): Unit =
//    js.native
//}
//
//@js.native
//@JSImport("firebase/firestore", "addDoc")
//object addDoc extends js.Function2[js.Dynamic, js.Object, js.Dynamic] {
//  def apply(col: js.Dynamic, data: js.Object): js.Dynamic = js.native
//}
//
//@js.native
//@JSImport("firebase/firestore", "deleteDoc")
//object deleteDoc extends js.Function1[js.Dynamic, js.Dynamic] {
//  def apply(doc: js.Dynamic): js.Dynamic = js.native
//}
//
//@js.native
//@JSImport("firebase/firestore", "doc")
//object doc extends js.Function3[js.Dynamic, String, String, js.Dynamic] {
//  def apply(db: js.Dynamic, path: String, id: String): js.Dynamic = js.native
//}
//
//@js.native
//@JSImport("firebase/firestore", "query")
//object query extends js.Function2[js.Dynamic, js.Dynamic, js.Dynamic] {
//  def apply(col: js.Dynamic, constraint: js.Dynamic): js.Dynamic = js.native
//}
//
//@js.native
//@JSImport("firebase/firestore", "orderBy")
//object orderBy extends js.Function1[String, js.Dynamic] {
//  def apply(field: String): js.Dynamic = js.native
//}
//
//@js.native
//@JSImport("firebase/auth", "getAuth")
//object getAuth extends js.Function1[js.Dynamic, js.Dynamic] {
//  def apply(app: js.Dynamic): js.Dynamic = js.native
//}
//
//@js.native
//@JSImport("firebase/auth", "signInWithCustomToken")
//object signInWithCustomToken extends js.Function2[js.Dynamic, String, js.Dynamic] {
//  def apply(auth: js.Dynamic, token: String): js.Dynamic = js.native
//}
//
//@js.native
//@JSImport("firebase/auth", "signInAnonymously")
//object signInAnonymously extends js.Function1[js.Dynamic, js.Dynamic] {
//  def apply(auth: js.Dynamic): js.Dynamic = js.native
//}
//
//// Model, Messages, and App logic for Tyrian
//object BudgetTracker extends TyrianApp[IO, Msg, Model] {
//  private val appId = js.Dynamic.global.__app_id
//    .asInstanceOf[js.UndefOr[String]]
//    .toOption
//    .getOrElse("default-app-id")
//  private val firebaseConfig = js.Dynamic.global.__firebase_config
//    .asInstanceOf[js.UndefOr[js.Object]]
//    .toOption
//    .getOrElse(js.Object())
//  private val initialAuthToken = js.Dynamic.global.__initial_auth_token
//    .asInstanceOf[js.UndefOr[String]]
//    .toOption
//
//  // Firestore & Auth handles
//  private var db: js.Dynamic   = _
//  private var auth: js.Dynamic = _
//  private var userId: String   = "anonymous"
//
//  sealed trait Msg
//  case object AddExpense                                    extends Msg
//  case class DescriptionChanged(value: String)              extends Msg
//  case class AmountChanged(value: String)                   extends Msg
//  case class PaidByChanged(value: String)                   extends Msg
//  case class SplitTypeChanged(value: String)                extends Msg
//  case class MyShareChanged(value: String)                  extends Msg
//  case class WifeShareChanged(value: String)                extends Msg
//  case class DeleteExpense(id: String)                      extends Msg
//  case class NewData(expenses: List[Expense])               extends Msg
//  case class SetUserId(id: String)                          extends Msg
//  case class ShowMessageBox(title: String, message: String) extends Msg
//  case object HideMessageBox                                extends Msg
//
//  case class Expense(
//      id: String,
//      description: String,
//      totalAmount: Double,
//      paidBy: String,
//      myShare: Double,
//      wifeShare: Double
//  )
//
//  case class Model(
//      description: String = "",
//      amount: String = "",
//      paidBy: String = "me",
//      splitType: String = "even",
//      myShare: String = "",
//      wifeShare: String = "",
//      expenses: List[Expense] = Nil,
//      balance: Double = 0.0,
//      showModal: Boolean = false,
//      modalTitle: String = "",
//      modalMessage: String = ""
//  )
//
//  def init(flags: Map[String, String]): (Model, Cmd[Msg]) = {
//    try {
//      val app = initializeApp(firebaseConfig)
//      auth = getAuth(app)
//      db = getFirestore(app)
//      Cmd.Run[js.Dynamic, SetUserId] {
//        if initialAuthToken.isDefined then {
//          signInWithCustomToken(auth, initialAuthToken.get)
//        } else {
//          signInAnonymously(auth)
//        }
//      } {
//        case Success(credential) =>
//          SetUserId(credential.user.uid.asInstanceOf[String]);
//        case Failure(e) =>
//          ShowMessageBox("Auth Error", s"Failed to sign in: ${e.getMessage}")
//      }
//    } catch {
//      case e: Exception =>
//        (
//          Model(),
//          Cmd.Run[Unit, Msg](() => throw e) {
//            case Success(_) =>
//              ShowMessageBox("Init Error", s"Failed to initialize Firebase: ${e.getMessage}")
//            case Failure(e) =>
//              ShowMessageBox("Init Error", s"Failed to initialize Firebase: ${e.getMessage}")
//          }
//        )
//    }
//    (Model(), Cmd.None)
//  }
//
//  def subscriptions(model: Model): Sub[Msg] = {
//    val q =
//      query(collection(db, s"artifacts/$appId/public/data/expenses"), orderBy("timestamp", "desc"))
//    Sub.ofFunction { listener =>
//      onSnapshot(
//        q,
//        (snapshot: js.Dynamic) => {
//          val expenses = snapshot.docs
//            .asInstanceOf[js.Array[js.Dynamic]]
//            .toList
//            .map { doc =>
//              val data = doc.data().asInstanceOf[js.Dynamic]
//              Expense(
//                doc.id.asInstanceOf[String],
//                data.description.asInstanceOf[String],
//                data.totalAmount.asInstanceOf[Double],
//                data.paidBy.asInstanceOf[String],
//                data.myShare.asInstanceOf[Double],
//                data.wifeShare.asInstanceOf[Double]
//              )
//            }
//          listener(NewData(expenses))
//        }
//      )
//    }
//  }
//
//  def update(model: Model): Msg => (Model, Cmd[Msg]) = {
//    case DescriptionChanged(value) =>
//      (model.copy(description = value), Cmd.None)
//    case AmountChanged(value)    => (model.copy(amount = value), Cmd.None)
//    case PaidByChanged(value)    => (model.copy(paidBy = value), Cmd.None)
//    case SplitTypeChanged(value) => (model.copy(splitType = value), Cmd.None)
//    case MyShareChanged(value)   => (model.copy(myShare = value), Cmd.None)
//    case WifeShareChanged(value) => (model.copy(wifeShare = value), Cmd.None)
//    case SetUserId(id) =>
//      userId = id
//      (model, Cmd.None)
//    case AddExpense =>
//      val totalAmount = model.amount.toDoubleOption.getOrElse(0.0)
//      if totalAmount <= 0 then {
//        (model.copy(amount = ""), ShowMessageBox("Invalid Amount", "Please enter a valid amount."))
//      } else {
//        val (myShare, wifeShare) = if model.splitType == "even" then {
//          (totalAmount / 2, totalAmount / 2)
//        } else {
//          val myShareVal   = model.myShare.toDoubleOption.getOrElse(0.0)
//          val wifeShareVal = model.wifeShare.toDoubleOption.getOrElse(0.0)
//          if myShareVal + wifeShareVal != totalAmount then {
//            return (
//              model,
//              ShowMessageBox("Invalid Split", "Shares must add up to the total amount.")
//            )
//          }
//          (myShareVal, wifeShareVal)
//        }
//
//        val expenseData = js.Dynamic
//          .literal(
//            description = model.description,
//            totalAmount = totalAmount,
//            paidBy = model.paidBy,
//            myShare = myShare,
//            wifeShare = wifeShare,
//            timestamp = new js.Date()
//          )
//        val expensesCollection =
//          collection(db, s"artifacts/$appId/public/data/expenses")
//        (
//          Model(balance = model.balance),
//          Cmd.Run[js.Dynamic, Msg] {
//            addDoc(expensesCollection, expenseData)
//          } {
//            case Success(_) =>
//              DescriptionChanged(""); AmountChanged(""); MyShareChanged("");
//              WifeShareChanged(""); SplitTypeChanged("even"); AddExpense
//            case Failure(e) =>
//              ShowMessageBox("Error", s"Failed to add expense: ${e.getMessage}")
//          }
//        )
//      }
//    case DeleteExpense(id) =>
//      val docRef = doc(db, s"artifacts/$appId/public/data/expenses", id)
//      (
//        model,
//        Cmd.Run[js.Dynamic, Msg] {
//          deleteDoc(docRef)
//        } {
//          case Success(_) => Logger.info("Expense deleted.")
//          case Failure(e) =>
//            ShowMessageBox("Error", s"Failed to delete expense: ${e.getMessage}")
//        }
//      )
//    case NewData(expenses) =>
//      val myTotalOwed = expenses.foldLeft(0.0) { (acc, exp) =>
//        if exp.paidBy == "me" then acc - exp.wifeShare
//        else acc + exp.myShare
//      }
//      (model.copy(expenses = expenses, balance = myTotalOwed), Cmd.None)
//    case ShowMessageBox(title, message) =>
//      (model.copy(showModal = true, modalTitle = title, modalMessage = message), Cmd.None)
//    case HideMessageBox =>
//      (model.copy(showModal = false), Cmd.None)
//  }
//
//  def view(model: Model): Html[Msg] =
//    div(`class` := "bg-gray-100 min-h-screen flex items-start justify-center p-4 md:p-8")(
//      div(`class` := "container w-full bg-white rounded-2xl shadow-lg p-6 flex flex-col gap-6")(
//        h1(`class` := "text-3xl font-bold text-gray-800 text-center")("Budget Tracker"),
//        div(
//          `class` := "flex items-center justify-center p-2 rounded-xl bg-green-500 text-white font-semibold"
//        )(
//          svg(
//            `class` := "w-6 h-6 mr-2",
//            fill    := "none",
//            stroke  := "currentColor",
//            viewBox := "0 0 24 24",
//            xmlns   := "http://www.w3.org/2000/svg"
//          )(
//            path(
//              `stroke-linecap`  := "round",
//              `stroke-linejoin` := "round",
//              `stroke-width`    := "2",
//              d := "M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8a4 4 0 01-4 4v2a2 2 0 002 2h4a2 2 0 002-2v-2a4 4 0 01-4-4zm0 0a4 4 0 004-4H8a4 4 0 004 4z"
//            )
//          ),
//          span(`class` := "text-lg")(
//            if model.balance > 0 then s"You owe your wife ${model.balance.toInt}円"
//            else if model.balance < 0 then s"Your wife owes you ${model.balance.toInt.abs}円"
//            else "You are all squared up!"
//          )
//        ),
//        div(`class` := "text-center text-gray-500 text-sm")(
//          span(`class` := "font-bold")("User ID: "),
//          span(userId)
//        ),
//        div(`class` := "bg-gray-50 p-4 rounded-xl shadow-inner")(
//          h2(`class` := "text-xl font-bold text-gray-700 mb-4")("Add New Expense"),
//          form(onSubmit(AddExpense), `class` := "space-y-4")(
//            div(
//              label(`class` := "block text-sm font-medium text-gray-600")("Description"),
//              input(
//                `type` := "text",
//                `class` := "mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:ring-green-500 focus:border-green-500 p-2",
//                placeholder := "e.g. Groceries, Dinner",
//                onInput(DescriptionChanged),
//                value := model.description
//              )
//            ),
//            div(
//              label(`class` := "block text-sm font-medium text-gray-600")("Amount (Yen)"),
//              input(
//                `type` := "number",
//                `class` := "mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:ring-green-500 focus:border-green-500 p-2",
//                placeholder := "e.g. 3000",
//                min         := "1",
//                onInput(AmountChanged),
//                value := model.amount
//              )
//            ),
//            div(`class` := "flex flex-col md:flex-row md:space-x-4 space-y-4 md:space-y-0")(
//              div(`class` := "flex-1")(
//                label(`class` := "block text-sm font-medium text-gray-600")("Paid By"),
//                select(
//                  `class` := "mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:ring-green-500 focus:border-green-500 p-2",
//                  onInput(PaidByChanged),
//                  value := model.paidBy
//                )(
//                  option(value := "me")("Me"),
//                  option(value := "wife")("Wife")
//                )
//              ),
//              div(`class` := "flex-1")(
//                label(`class` := "block text-sm font-medium text-gray-600")("Split Type"),
//                select(
//                  `class` := "mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:ring-green-500 focus:border-green-500 p-2",
//                  onInput(SplitTypeChanged),
//                  value := model.splitType
//                )(
//                  option(value := "even")("Even Split"),
//                  option(value := "custom")("Custom Amount")
//                )
//              )
//            ),
//            if model.splitType == "custom" then
//              div(`class` := "space-y-4")(
//                div(
//                  label(`class` := "block text-sm font-medium text-gray-600")("My Share (Yen)"),
//                  input(
//                    `type` := "number",
//                    `class` := "mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:ring-green-500 focus:border-green-500 p-2",
//                    placeholder := "e.g. 2000",
//                    min         := "0",
//                    onInput(MyShareChanged),
//                    value := model.myShare
//                  )
//                ),
//                div(
//                  label(`class` := "block text-sm font-medium text-gray-600")("Wife's Share (Yen)"),
//                  input(
//                    `type` := "number",
//                    `class` := "mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:ring-green-500 focus:border-green-500 p-2",
//                    placeholder := "e.g. 1000",
//                    min         := "0",
//                    onInput(WifeShareChanged),
//                    value := model.wifeShare
//                  )
//                )
//              )
//            else div(),
//            button(
//              `type` := "submit",
//              `class` := "w-full bg-green-600 text-white font-bold py-3 px-4 rounded-xl shadow-md hover:bg-green-700 transition duration-300"
//            )("Add Expense")
//          )
//        ),
//        div(`class` := "bg-white p-4 rounded-xl shadow-inner")(
//          h2(`class` := "text-xl font-bold text-gray-700 mb-4")("Expense History"),
//          div(`class` := "space-y-4")(
//            if model.expenses.isEmpty then
//              p(`class` := "text-center text-gray-500")("No expenses added yet.")
//            else
//              model.expenses.map { expense =>
//                val paidByDisplay = if expense.paidBy == "me" then "Me" else "Wife"
//                val owedTo        = if expense.paidBy == "me" then "Wife" else "Me"
//                val owes =
//                  if expense.paidBy == "me" then expense.wifeShare
//                  else expense.myShare
//                div(`class` := "bg-white p-4 rounded-xl shadow-sm border border-gray-200")(
//                  div(`class` := "flex justify-between items-center mb-2")(
//                    span(`class` := "font-bold text-gray-800")(expense.description),
//                    span(`class` := "text-lg font-bold text-green-600")(
//                      s"${expense.totalAmount.toInt}円"
//                    )
//                  ),
//                  p(`class` := "text-sm text-gray-500")(s"Paid by: ${paidByDisplay}"),
//                  p(`class` := "text-sm text-gray-500")(
//                    s"My share: ${expense.myShare.toInt}円 | Wife's share: ${expense.wifeShare.toInt}円"
//                  ),
//                  div(`class` := "mt-2 flex items-center justify-between")(
//                    p(`class` := "text-sm font-semibold text-gray-700")(
//                      s"${owedTo} owes: ${owes.toInt}円"
//                    ),
//                    button(
//                      `class` := "delete-btn text-red-500 hover:text-red-700 transition duration-300",
//                      onClick(DeleteExpense(expense.id)),
//                      title := "Delete Expense"
//                    )(
//                      svg(
//                        `class` := "w-5 h-5",
//                        fill    := "none",
//                        stroke  := "currentColor",
//                        viewBox := "0 0 24 24",
//                        xmlns   := "http://www.w3.org/2000/svg"
//                      )(
//                        path(
//                          `stroke-linecap`  := "round",
//                          `stroke-linejoin` := "round",
//                          `stroke-width`    := "2",
//                          d := "M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"
//                        )
//                      )
//                    )
//                  )
//                )
//              }
//          )
//        ),
//        if model.showModal then
//          div(
//            `class` := "fixed inset-0 z-50 flex items-center justify-center bg-gray-900 bg-opacity-50"
//          )(
//            div(`class` := "bg-white p-6 rounded-lg shadow-xl max-w-sm w-full")(
//              h3(`class` := "text-lg font-semibold text-gray-800 mb-2")(model.modalTitle),
//              p(`class` := "text-sm text-gray-600 mb-4")(model.modalMessage),
//              div(`class` := "flex justify-end space-x-2")(
//                button(
//                  `class` := "bg-green-600 text-white font-bold py-2 px-4 rounded-md hover:bg-green-700 transition duration-300",
//                  onClick(HideMessageBox)
//                )("OK")
//              )
//            )
//          )
//        else div()
//      )
//    )
//
//  def onEvent(e: dom.Event): Option[Msg] =
//    e match {
//      case _ => None
//    }
//}
