package model

import org.json.JSONObject

data class Redirection(
	val fromFQDN: String,
	val toURI: String,
	val isHttps: Boolean
)

fun Redirection.toJSON(): JSONObject {
	val json = JSONObject()

	json.put("fromFQDN", fromFQDN)
	json.put("toURI", toURI)
	json.put("isHttps", isHttps)

	return json
}

fun jsonObjectToRedirection(json: JSONObject) = Redirection(
	json.getString("fromFQDN"),
	json.getString("toURI"),
	json.getBoolean("isHttps")
)