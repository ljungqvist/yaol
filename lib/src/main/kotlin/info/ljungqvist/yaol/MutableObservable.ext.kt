package info.ljungqvist.yaol


fun <T, OUT> List<MutableObservable<T>>.twoWayJoin(
        mapping: (List<T>) -> OUT,
        reverseMapping: (OUT) -> List<T>
): MutableObservable<OUT> =
        TwoWayMappedObservable(
                { mapping(map { it.value }) },
                {
                    reverseMapping(it).forEachIndexed { i, t ->
                        this[i].value = t
                    }
                }
        ).also { mapped ->
            forEach { it.addMappedObservables(mapped) }
        }
