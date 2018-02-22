package support

import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import kotlin.concurrent.thread

fun <T> task(predicate: () -> T) = promise<T> { resolve, reject ->
	try {
		resolve(predicate())
	} catch (e: Exception) {
		reject(e)
	}
}

fun <T> promise(
	predicate: (
		resolve: (response: T) -> Unit,
		reject: (error: Throwable) -> Unit
	) -> Unit
): Observable<T> {
	val subject = PublishSubject.create<T>()

	fun complete(result: T) {
		subject.onNext(result)
		subject.onComplete()
	}

	fun error(error: Throwable) {
		subject.onError(error)
		subject.onComplete()
	}

	thread {
		try {
			predicate(::complete, ::error)
		} catch (e: Exception) {
			error(e)
		}
	}

	return subject.share()
}
