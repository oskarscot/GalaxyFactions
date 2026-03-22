package scot.oskar.galaxyfactions.data

interface Repository<KEY, RESULT> {

    suspend fun findById(id: KEY): RESULT?
    suspend fun findAll(): List<RESULT>

}