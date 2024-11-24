package models

import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import io.circe.generic.auto._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._

trait JsonSupport extends FailFastCirceSupport {
  // No additional code needed as Circe auto-derivation handles the models
}


