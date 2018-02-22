package commands

import clients.Manager
import io.reactivex.Observable
import io.reactivex.functions.BiFunction
import model.Redirection
import model.jsonObjectToRedirection
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess

fun bulkCreate() {
	var stdin = ""
	var line: String? = ""

	while ({ line = readLine(); line != null }()) {
		stdin += line
	}

	val redirections = try {
		val json = JSONArray(stdin)
		json.map { jsonObjectToRedirection(it as JSONObject) }
	} catch (e: JSONException) {
		println("The stdin is not correctly formatted!")
		exitProcess(1)
	}

	Observable
		.zip<Long, Redirection, Redirection>(
			Observable.interval(200, TimeUnit.MILLISECONDS),
			Observable
				.just(redirections)
				.flatMapIterable { it },
			BiFunction { _, r -> r }
		)
		.doOnNext { println("Creating ${it.fromFQDN} --> ${it.toURI}...") }
		.flatMap {
			val (protocol, uri) = """(http|https)://(.+)"""
				.toRegex()
				.matchEntire(it.toURI)
				?.destructured
				?: throw Error("The uri ${it.toURI} is not valid!\n" +
					"Make sure that is in the form of: http(s)://www.example.com")

			Manager.createRedirection(it.fromFQDN, protocol, uri)
		}
		// We convert this to future so the current thread doesn't exit before
		// waiting for the redirections to be created
		.toList()
		.toFuture()
		.get()

	exitProcess(0)
}

fun bulkCreateHelp() {
	println(
		"""Creates redirections in bulk
			|
			|Creates redirections in bulk, taking the input from the stdin. The
			|input should be a JSON string representing an array of
			|redirections. Each redirection should be a JSON object consisting
			|of two properties: fromFQDN and toURI, like this:
			|
			|{
			|   "fromFQDN": "test.example.com",
			|   "toURI": "https://www.example.com/test"
			|}
			|
			|For more information, run aws-redirect-manager --help
			|
			|Usage: aws-redirect-manager bulk-create
		""".trimMargin()
	)
}