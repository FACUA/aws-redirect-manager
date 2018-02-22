package extensions

import com.amazonaws.services.s3.AmazonS3

val AmazonS3.endpoint get() =
	"s3-website-${this.regionName}.amazonaws.com"

/**
 * Generate this code by running:
 *
 * copy(Array
 *     .from(document.querySelectorAll("#w113aab7d223c13b7 tbody tr"))
 *     .slice(1)
 *     .map(it => ({
 *         endpoint: it.children[1].textContent.trim(),
 *         value: it.children[2].textContent.trim()})
 *     )
 *     .filter(it => it.value !== "Not supported")
 *     .map(it => `"${it.endpoint}" -> "${it.value}"`)
 *     .join("\n")
 * )
 *
 * On:
 *
 * https://docs.aws.amazon.com/general/latest/gr/rande.html#s3_region
 *
 */
val AmazonS3.hostedZone get() = when (this.endpoint) {
	"s3-website.us-east-2.amazonaws.com" -> "Z2O1EMRO9K5GLX"
	"s3-website-us-east-1.amazonaws.com" -> "Z3AQBSTGFYJSTF"
	"s3-website-us-west-1.amazonaws.com" -> "Z2F56UZL2M1ACD"
	"s3-website-us-west-2.amazonaws.com" -> "Z3BJ6K6RIION7M"
	"s3-website.ca-central-1.amazonaws.com" -> "Z1QDHH18159H29"
	"s3-website.ap-south-1.amazonaws.com" -> "Z11RGJOFQNVJUP"
	"s3-website.ap-northeast-2.amazonaws.com" -> "Z3W03O7B5YMIYP"
	"s3-website.ap-northeast-3.amazonaws.com" -> "Z2YQB5RD63NC85"
	"s3-website-ap-southeast-1.amazonaws.com" -> "Z3O0J2DXBE1FTB"
	"s3-website-ap-southeast-2.amazonaws.com" -> "Z1WCIGYICN2BYD"
	"s3-website-ap-northeast-1.amazonaws.com" -> "Z2M4EHUR26P7ZW"
	"s3-website.eu-central-1.amazonaws.com" -> "Z21DNDUVLTQW6Q"
	"s3-website-eu-west-1.amazonaws.com" -> "Z1BKCTXD74EZPE"
	"s3-website.eu-west-2.amazonaws.com" -> "Z3GKZC51ZF0DB4"
	"s3-website.eu-west-3.amazonaws.com" -> "Z3R1K369G5AVDG"
	"s3-website-sa-east-1.amazonaws.com" -> "Z7KQH4QJS55SO"
	else -> throw Error("Endpoint $endpoint not supported!")
}