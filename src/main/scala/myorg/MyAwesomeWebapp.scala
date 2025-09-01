package myorg

import cats.effect.IO
import myorg.MyAwesomeWebapp.{Model, Msg}
import tyrian.Html.*
import tyrian.*
import tyrian.SVG.*

import scala.scalajs.js.annotation.*

@JSExportTopLevel("TyrianApp")
object MyAwesomeWebapp extends TyrianIOApp[Msg, Model] {

  private val userId: String = "me"

  override def router: Location => Msg =
    Routing.none(Msg.NoOp)

  override def init(flags: Map[String, String]): (Model, Cmd[IO, Msg]) =
    (Model(), Cmd.None)

  override def update(model: Model): Msg => (Model, Cmd[IO, Msg]) = msg => {
    println(s"receiving message: $msg")
    msg match {
      case Msg.AddExpense =>

        val amountE =
          if model.amount <= 0 then Left(Error.InvalidAmount)
          else Right(model.amount)

        val sharesE = model.splitType match {
          case SplitType.Even => Right((model.amount / 2, model.amount / 2))
          case c: SplitType.Custom if model.amount == c.myShare + c.wifeShare =>
            Right((c.myShare, c.wifeShare))
          case c: SplitType.Custom =>
            Left(Error.InvalidSplit)
        }

        val descriptionE =
          if model.description.trim.isEmpty then Left(Error.InvalidDescription)
          else Right(model.description)

        val updatedModelE = for {
          amount               <- amountE
          (myShare, wifeShare) <- sharesE
          description          <- descriptionE
          expenses = Expense(
            id = model.runningId.toString,
            description = model.description,
            totalAmount = model.amount.toDouble,
            paidBy = model.paidBy,
            myShare = myShare.toDouble,
            wifeShare = wifeShare.toDouble
          ) :: model.expenses
        } yield model.copy(
          splitType = SplitType.Even,
          description = "",
          amount = 0,
          runningId = model.runningId + 1,
          expenses = expenses
        )

        updatedModelE match {
          case Right(updatedModel) => (updatedModel, Cmd.None)
          case Left(error) =>
            (model, Cmd.Emit(Msg.ShowMessageBox(title = error.title, message = error.message)))
        }

      case Msg.AmountChanged(amount) => (model.copy(amount = amount), Cmd.None)
      case Msg.DeleteExpense(id) =>
        (model.copy(expenses = model.expenses.filterNot(_.id == id)), Cmd.None)
      case Msg.DescriptionChanged(desc) => (model.copy(description = desc), Cmd.None)
      case Msg.HideMessageBox =>
        (model.copy(showModal = false, modalTitle = "", modalMessage = ""), Cmd.None)
      case Msg.MyShareChanged(amount) =>
        (
          model.splitType match {
            case SplitType.Even      => model
            case c: SplitType.Custom => model.copy(splitType = c.copy(myShare = amount))
          },
          Cmd.None
        )
      case Msg.NewData(expenses)     => (model.copy(expenses = expenses), Cmd.None)
      case Msg.NoOp                  => (model, Cmd.None)
      case Msg.PaidByChanged(paidBy) => (model.copy(paidBy = paidBy), Cmd.None)
      case Msg.SetUserId(_)          => (model, Cmd.None) // TODO
      case Msg.ShowMessageBox(title, message) =>
        (model.copy(showModal = true, modalTitle = title, modalMessage = message), Cmd.None)
      case Msg.SplitTypeChanged(splitType) =>
        (model.copy(splitType = SplitType.from(splitType, model.amount)), Cmd.None)
      case Msg.WifeShareChanged(amount) =>
        (
          model.splitType match {
            case SplitType.Even      => model
            case c: SplitType.Custom => model.copy(splitType = c.copy(wifeShare = amount))
          },
          Cmd.None
        )

    }
  }

  private def newExpenseFormDescription(model: Model) = div(
    label(`class` := "block text-sm font-medium text-gray-600")("Description"),
    input(
      `type` := "text",
      `class` := "mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:ring-green-500 focus:border-green-500 p-2",
      placeholder := "e.g. Groceries, Dinner",
      onInput(Msg.DescriptionChanged.apply),
      value := model.description
    )
  )

  private def newExpenseFormAmount(model: Model): Html[Msg] = div(
    label(`class` := "block text-sm font-medium text-gray-600")("Amount (Yen)"),
    input(
      `type` := "number",
      `class` := "mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:ring-green-500 focus:border-green-500 p-2",
      placeholder := "e.g. 3000",
      min         := "1",
      onInput(amount => Msg.AmountChanged.apply(BigDecimal(amount))), // TODO handle
      value := model.amount.toString()
    )
  )

  private def newExpenseFormPaidBy(model: Model) = div(`class` := "flex-1")(
    label(`class` := "block text-sm font-medium text-gray-600")("Paid By"),
    select(
      `class` := "mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:ring-green-500 focus:border-green-500 p-2",
      onInput(Msg.PaidByChanged.apply),
      value := model.paidBy
    )(
      option(value := "me")("Me"),
      option(value := "wife")("Wife")
    )
  )

  private def newExpenseFormSplitType(model: Model) = div(`class` := "flex-1")(
    label(`class` := "block text-sm font-medium text-gray-600")("Split Type"),
    select(
      `class` := "mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:ring-green-500 focus:border-green-500 p-2",
      onInput(Msg.SplitTypeChanged.apply),
      value := model.splitType.toString
    )(
      option(value := "even")("Even Split"),
      option(value := "custom")("Custom Amount")
    )
  )

  private def newCustomSplitInputs(model: Model) = model.splitType match {
    case custom: SplitType.Custom =>
      div(`class` := "space-y-4")(
        div(
          label(`class` := "block text-sm font-medium text-gray-600")("Guy's Share (Yen)"),
          input(
            `type` := "number",
            `class` := "mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:ring-green-500 focus:border-green-500 p-2",
            placeholder := "e.g. 2000",
            min         := "0",
            onInput(amount => Msg.MyShareChanged.apply(BigDecimal(amount))),
            value := custom.myShare.toString
          )
        ),
        div(
          label(`class` := "block text-sm font-medium text-gray-600")("Kan's Share (Yen)"),
          input(
            `type` := "number",
            `class` := "mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:ring-green-500 focus:border-green-500 p-2",
            placeholder := "e.g. 1000",
            min         := "0",
            onInput(amount => Msg.WifeShareChanged.apply(BigDecimal(amount))),
            value := custom.wifeShare.toString()
          )
        )
      )
    case SplitType.Even => div()
  }

  private def newExpenseForm(model: Model) =
    div(`class` := "bg-gray-50 p-4 rounded-xl shadow-inner")(
      h2(`class` := "text-xl font-bold text-gray-700 mb-4")("Add New Expense"),
      form(onSubmit(Msg.AddExpense), `class` := "space-y-4")(
        newExpenseFormDescription(model),
        newExpenseFormAmount(model),
        div(`class` := "flex flex-col md:flex-row md:space-x-4 space-y-4 md:space-y-0")(
          newExpenseFormPaidBy(model),
          newExpenseFormSplitType(model)
        ),
        newCustomSplitInputs(model),
        button(
          `type` := "submit",
          `class` := "w-full bg-green-600 text-white font-bold py-3 px-4 rounded-xl shadow-md hover:bg-green-700 transition duration-300"
        )("Add Expense")
      )
    )

  private def headerView(model: Model) = div(
    `class` := "flex items-center justify-center p-2 rounded-xl bg-green-500 text-white font-semibold"
  )(
    svg(
      `class` := "w-6 h-6 mr-2",
      fill    := "none",
      stroke  := "currentColor",
      viewBox := "0 0 24 24",
      xmlns   := "http://www.w3.org/2000/svg"
    )(
      path(
        AttributeNameString("stroke-linecap")  := "round",
        AttributeNameString("stroke-linejoin") := "round",
        AttributeNameString("stroke-width")    := "2",
        d := "M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8a4 4 0 01-4 4v2a2 2 0 002 2h4a2 2 0 002-2v-2a4 4 0 01-4-4zm0 0a4 4 0 004-4H8a4 4 0 004 4z"
      )
    ),
    span(`class` := "text-lg")(
      if model.balance > 0 then s"You owe your wife ${model.balance.toInt}円"
      else if model.balance < 0 then s"Your wife owes you ${model.balance.toInt.abs}円"
      else "You are all squared up!"
    )
  )

  private def messageBoxModal(model: Model) = if model.showModal then
    div(
      `class` := "fixed inset-0 z-50 flex items-center justify-center bg-gray-900 bg-opacity-50"
    )(
      div(`class` := "bg-white p-6 rounded-lg shadow-xl max-w-sm w-full")(
        h3(`class` := "text-lg font-semibold text-gray-800 mb-2")(model.modalTitle),
        p(`class` := "text-sm text-gray-600 mb-4")(model.modalMessage),
        div(`class` := "flex justify-end space-x-2")(
          button(
            `class` := "bg-green-600 text-white font-bold py-2 px-4 rounded-md hover:bg-green-700 transition duration-300",
            onClick(Msg.HideMessageBox)
          )("OK")
        )
      )
    )
  else div()

  private def expenseHistory(model: Model) = div(`class` := "bg-white p-4 rounded-xl shadow-inner")(
    h2(`class` := "text-xl font-bold text-gray-700 mb-4")("Expense History"),
    div(`class` := "space-y-4")(
      if model.expenses.isEmpty then {
        List(
          Tag(
            "p",
            List(`class` := "text-center text-gray-500"),
            List(text("No expenses added yet."))
          )
        )
      } else {
        model.expenses.map { expense =>
          val paidByDisplay = if expense.paidBy == "me" then "Me" else "Wife"
          val owedTo        = if expense.paidBy == "me" then "Wife" else "Me"
          val owes =
            if expense.paidBy == "me" then expense.wifeShare
            else expense.myShare
          div(`class` := "bg-white p-4 rounded-xl shadow-sm border border-gray-200")(
            div(`class` := "flex justify-between items-center mb-2")(
              span(`class` := "font-bold text-gray-800")(expense.description),
              span(`class` := "text-lg font-bold text-green-600")(
                s"${expense.totalAmount.toInt}円"
              )
            ),
            p(`class` := "text-sm text-gray-500")(s"Paid by: ${paidByDisplay}"),
            p(`class` := "text-sm text-gray-500")(
              s"My share: ${expense.myShare.toInt}円 | Wife's share: ${expense.wifeShare.toInt}円"
            ),
            div(`class` := "mt-2 flex items-center justify-between")(
              p(`class` := "text-sm font-semibold text-gray-700")(
                s"${owedTo} owes: ${owes.toInt}円"
              ),
              button(
                `class` := "delete-btn text-red-500 hover:text-red-700 transition duration-300",
                onClick(Msg.DeleteExpense(expense.id)),
                title := "Delete Expense"
              )(
                svg(
                  `class` := "w-5 h-5",
                  fill    := "none",
                  stroke  := "currentColor",
                  viewBox := "0 0 24 24",
                  xmlns   := "http://www.w3.org/2000/svg"
                )(
                  path(
                    AttributeNameString("stroke-linecap")  := "round",
                    AttributeNameString("stroke-linejoin") := "round",
                    AttributeNameString("stroke-width")    := "2",
                    d := "M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"
                  )
                )
              )
            )
          )
        }
      }
    )
  )

  override def view(model: Model): Html[Msg] =
    div(`class` := "bg-gray-100 min-h-screen flex items-start justify-center p-4 md:p-8")(
      div(`class` := "container w-full bg-white rounded-2xl shadow-lg p-6 flex flex-col gap-6")(
        h1(`class` := "text-3xl font-bold text-gray-800 text-center")("Budget Tracker"),
        headerView(model),
        div(`class` := "text-center text-gray-500 text-sm")(
          span(`class` := "font-bold")("User ID: "),
          span(userId)
        ),
        newExpenseForm(model),
        expenseHistory(model),
        messageBoxModal(model)
      )
    )

  def subscriptions(model: Model): Sub[IO, Msg] =
    Sub.None

  sealed trait SplitType
  object SplitType:
    case object Even extends SplitType {
      override def toString: String = "even"
    }
    case class Custom(myShare: BigDecimal, wifeShare: BigDecimal) extends SplitType {
      override def toString: String = "custom"
    }

    def from(splitType: String, amount: BigDecimal): SplitType =
      splitType match {
        case "custom" => Custom(myShare = amount, wifeShare = 0)
        case _        => Even
      }

  sealed trait Msg
  object Msg:
    case class AmountChanged(value: BigDecimal)               extends Msg
    case class DeleteExpense(id: String)                      extends Msg
    case class DescriptionChanged(value: String)              extends Msg
    case class MyShareChanged(value: BigDecimal)              extends Msg
    case class NewData(expenses: List[Expense])               extends Msg
    case class PaidByChanged(value: String)                   extends Msg
    case class SetUserId(id: String)                          extends Msg
    case class ShowMessageBox(title: String, message: String) extends Msg
    case class SplitTypeChanged(value: String)                extends Msg
    case class WifeShareChanged(value: BigDecimal)            extends Msg
    case object AddExpense                                    extends Msg
    case object HideMessageBox                                extends Msg
    case object NoOp                                          extends Msg

  case class Expense(
      id: String,
      description: String,
      totalAmount: Double,
      paidBy: String,
      myShare: Double,
      wifeShare: Double
  )

  case class Model(
      description: String = "",
      amount: BigDecimal = BigDecimal(0),
      paidBy: String = "me",
      splitType: SplitType = SplitType.Even,
      expenses: List[Expense] = Nil,
      showModal: Boolean = false,
      modalTitle: String = "",
      modalMessage: String = "",
      runningId: Long = 1
  ) {
    val balance: BigDecimal =
      expenses.foldLeft(BigDecimal(0)) { case (amount, expense) =>
        amount + (if expense.paidBy == userId then -expense.wifeShare
                  else expense.myShare)
      }
  }

  sealed trait Error {
    val title: String
    val message: String
  }
  object Error:
    case object InvalidAmount extends Error {
      override val title: String   = "Invalid Amount"
      override val message: String = "Please enter a valid amount."
    }
    case object InvalidSplit extends Error {
      override val title: String   = "Invalid Split"
      override val message: String = "Shares must add up to the total amount."
    }
    case object InvalidDescription extends Error {
      override val title: String   = "Invalid Description"
      override val message: String = "Please enter a description."
    }

}
