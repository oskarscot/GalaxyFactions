package scot.oskar.galaxyfactions.config

import gg.ginco.jellyparty.codec.annotations.SerializableObject

@SerializableObject
class FactionsPluginConfig {

    var jdbcUrl: String = "jdbc:postgresql://localhost:5432/galaxyfactions"
    var username: String = "galaxyfactions"
    var password: String = "galaxyfactions"

}