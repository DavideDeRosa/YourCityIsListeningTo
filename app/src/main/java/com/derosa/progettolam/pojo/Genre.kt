package com.derosa.progettolam.pojo

import kotlin.reflect.KProperty

data class Genre(
    val `60s`: Double,
    val `70s`: Double,
    val `80s`: Double,
    val `90s`: Double,
    val acidjazz: Double,
    val alternative: Double,
    val alternativerock: Double,
    val ambient: Double,
    val atmospheric: Double,
    val blues: Double,
    val bluesrock: Double,
    val bossanova: Double,
    val breakbeat: Double,
    val celtic: Double,
    val chanson: Double,
    val chillout: Double,
    val choir: Double,
    val classical: Double,
    val classicrock: Double,
    val club: Double,
    val contemporary: Double,
    val country: Double,
    val dance: Double,
    val darkambient: Double,
    val darkwave: Double,
    val deephouse: Double,
    val disco: Double,
    val downtempo: Double,
    val drumnbass: Double,
    val dub: Double,
    val dubstep: Double,
    val easylistening: Double,
    val edm: Double,
    val electronic: Double,
    val electronica: Double,
    val electropop: Double,
    val ethno: Double,
    val eurodance: Double,
    val experimental: Double,
    val folk: Double,
    val funk: Double,
    val fusion: Double,
    val groove: Double,
    val grunge: Double,
    val hard: Double,
    val hardrock: Double,
    val hiphop: Double,
    val house: Double,
    val idm: Double,
    val improvisation: Double,
    val indie: Double,
    val industrial: Double,
    val instrumentalpop: Double,
    val instrumentalrock: Double,
    val jazz: Double,
    val jazzfusion: Double,
    val latin: Double,
    val lounge: Double,
    val medieval: Double,
    val metal: Double,
    val minimal: Double,
    val newage: Double,
    val newwave: Double,
    val orchestral: Double,
    val pop: Double,
    val popfolk: Double,
    val poprock: Double,
    val postrock: Double,
    val progressive: Double,
    val psychedelic: Double,
    val punkrock: Double,
    val rap: Double,
    val reggae: Double,
    val rnb: Double,
    val rock: Double,
    val rocknroll: Double,
    val singersongwriter: Double,
    val soul: Double,
    val soundtrack: Double,
    val swing: Double,
    val symphonic: Double,
    val synthpop: Double,
    val techno: Double,
    val trance: Double,
    val triphop: Double,
    val world: Double,
    val worldfusion: Double
) {
    fun getMaxGenre(): Pair<String, Double> {
        return getMax()
    }

    private fun getMax(): Pair<String, Double> {
        var maxKey = ""
        var maxValue = Double.MIN_VALUE
        this::class.members.forEach { member ->
            if (member is KProperty<*>) {
                val value = member.call(this) as? Double ?: return@forEach
                if (value > maxValue) {
                    maxValue = value
                    maxKey = member.name
                }
            }
        }
        return Pair(maxKey, maxValue)
    }
}