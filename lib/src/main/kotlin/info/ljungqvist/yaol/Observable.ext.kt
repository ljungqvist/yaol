package info.ljungqvist.yaol

fun <T> Observable<Observable<T>>.flatten(): Observable<T> = flatMap { it }

fun <T, OUT> List<Observable<T>>.join(mapping: (List<T>) -> OUT): Observable<OUT> =
        MappedObservable { mapping(map { it.value }) }
                .also { mapped ->
                    forEach { it.addMappedObservables(mapped) }
                }
