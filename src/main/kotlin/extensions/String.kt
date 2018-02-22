package extensions

val String.isS3Redirection get(): Boolean =
	"s3-website-[a-z0-9-]+\\.amazonaws\\.com\\."
		.toRegex()
		.matches(this)

/**
 * Hosted zone record sets have a trailing dot, like this:
 * example.com.
 * and we want:
 * example.com
 */
val String.s3BucketName get(): String = "(.+)\\."
	.toRegex()
	.matchEntire(this)
	?.groups
	?.get(1)
	?.value ?: this

/**
 * Gets the root domain from a string representing a subdomain, like this:
 * foo.bar.example.com --> example.com
 *
 * The string itself might also be the root domain itself:
 * example.com --> example.con
 */
val String.rootDomain get(): String = "([a-z0-9|-]+\\.)*([a-z0-9|-]+\\.[a-z]+)"
	.toRegex()
	.matchEntire(this)
	?.groups
	?.get(2)
	?.value ?: this