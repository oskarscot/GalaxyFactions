package scot.oskar.galaxyfactions.data

interface Repository<KEY, RESULT> {

    fun findById(id: KEY): RESULT?
    fun findAll(): List<RESULT>

}