// FULL_JDK
// USE_STDLIB
// FULL_VIPER_DUMP

import org.jetbrains.kotlin.formver.plugin.*


@Unique
fun <!VIPER_TEXT!>testConstructor<!>() : IntArray {
    postconditions<IntArray> { res ->
        res.size == 5
        forAll<Int> { index ->
            (0 <= index && index <= 3) implies (res[index] < res[index + 1])
        }
    }


    val array = IntArray(5)
    array.set(1, 1)
    array.set(2, 2)
    array.set(3, 3)
    array.set(4, 4)

    return array

}


@Unique
fun <!VIPER_TEXT!>extend<!>(@Unique @Borrowed list: IntArray, data : Int) : IntArray {
    postconditions<IntArray> { res ->
        list.size + 1 == res.size
        forAll<Int> { index ->
            (0 <= index && index < list.size) implies (list[index] == res[index])
        }
        res[res.size - 1] == data
    }


    val size = list.size
    val newList = IntArray(size + 1)
    verify(list.size + 1 == newList.size)
    var i = 0
    while (i < list.size) {
        loopInvariants {
            list.size + 1 == newList.size
            i >= 0 && i <= list.size
            i >= 0 && i + 1 <= newList.size

            forAll<Int> { index ->
                (0 <= index && index < i) implies (newList[index] == list[index])
            }
        }
        verify(i < list.size)
        verify(i + 1 < newList.size)
        val read = list[i]
        newList.set(i, read)
        i = i + 1
    }
    verify(i < newList.size)
    newList.set(i, data)
    return newList
}


@Unique
fun <!VIPER_TEXT!>createFirstNInts<!>(n: Int) : IntArray {
    preconditions {
        n >= 0
    }
    postconditions<IntArray> { res ->
        res.size == n
        forAll<Int> { index ->
            (0 <= index && index < res.size) implies (res[index] == index)
        }
    }

    val array = IntArray(n)
    verify(n >= 0)
    verify(n == array.size)

    var i = 0
    verify(
        forAll<Int> { index ->
            (0 <= index && index < i) implies (array[index] == index)
        }
    )
    while (i < n) {
        loopInvariants {
            n == array.size
            0 <= i && i <= n
            forAll<Int> { index ->
                (0 <= index && index < i) implies (array[index] == index)
            }
        }
        verify(i < array.size)
        array.set(i, i)
        val res = array[i] == i
        verify(res)
        i = i + 1
    }

    return array

}
