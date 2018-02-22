package support

import io.reactivex.Observable
import io.reactivex.Single

fun errorHandler(observable: Observable<String>) {
	errorHandler(observable.single(""))
}

fun errorHandler(observable: Single<String>) {
	observable.subscribe(
		{
			if (it != "") {
				println(it)
			}
		},
		Throwable::printStackTrace
	)
}