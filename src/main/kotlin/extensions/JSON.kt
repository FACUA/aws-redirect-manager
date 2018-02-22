package extensions

import org.json.JSONArray
import org.json.JSONObject

fun List<JSONObject>.toJSONArray(): JSONArray {
	val array = JSONArray()
	this.forEach { array.put(it) }
	return array
}