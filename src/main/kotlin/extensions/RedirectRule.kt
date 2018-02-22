package extensions

import com.amazonaws.services.s3.model.RedirectRule

val RedirectRule.uri get() = "${this.getprotocol()}://${this.hostName}"