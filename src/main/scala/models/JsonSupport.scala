package models

import io.circe.generic.auto._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport

trait JsonSupport extends FailFastCirceSupport
