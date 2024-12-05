package scratchpad

case class ServicesResponse(queryTermsJson: String, alertsJson: String)

case class Content(
                    text: String,
                    `type`: String,
                    language: String,
                  ) {
  lazy val textLc = text.toLowerCase
}

case class Alert(
                  id: String,
                  contents: Vector[Content],
                  date: String,
                  inputType: String,
                ) {
  lazy val allWords: Set[String] = contents.toArray.flatMap(_.textLc.split(" ")).toSet
}

case class QueryTerm(
                      id: Long,
                      target: Long,
                      text: String,
                      language: String,
                      keepOrder: Boolean,
                    ) {
  lazy val textLc = text.toLowerCase
  lazy val words = text.toLowerCase.split(" ").toSet

  def matchesAlert(alert: Alert): Boolean =
    words.forall(word => alert.allWords.contains(word)) &&
      (!keepOrder || alert.contents.exists(_.textLc.contains(textLc)))
}

case class Match(
                  alertId: String,
                  queryTermId: Long,
                ) {
  def outputFormatted = s"$alertId,$queryTermId"
}
