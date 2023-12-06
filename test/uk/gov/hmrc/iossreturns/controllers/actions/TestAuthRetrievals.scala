package uk.gov.hmrc.iossreturns.controllers.actions

import uk.gov.hmrc.auth.core.retrieve.~

object TestAuthRetrievals {
  implicit class Ops[A](a: A) {
    def ~[B](b: B): A ~ B = new ~(a, b)
  }
}
