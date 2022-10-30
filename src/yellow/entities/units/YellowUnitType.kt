package yellow.entities.units

import arc.func.Prov
import arc.graphics.Color
import arc.graphics.g2d.Draw
import arc.graphics.g2d.Fill
import arc.graphics.g2d.Lines
import arc.math.Mathf
import arc.scene.ui.layout.Table
import arc.util.Time
import arc.util.Tmp
import mindustry.Vars
import mindustry.graphics.Layer
import mindustry.type.UnitType
import mindustry.ui.Styles
import mindustry.world.meta.Stat
import mindustry.world.meta.StatUnit
import yellow.entities.units.entity.YellowUnitEntity
import yellow.internal.util.YellowUtilsKt
import yellow.internal.util.YellowUtilsKt.seperator
import yellow.internal.util.ins
import yellow.type.NameableWeapon
import yellow.world.meta.YellowStats

open class YellowUnitType(name: String): UnitType(name) {

    var maxLives = 5

    init {
        constructor = Prov<mindustry.gen.Unit> {YellowUnitEntity()}
    }

    override fun draw(unit: mindustry.gen.Unit) {
        super.draw(unit)

        var s = Mathf.absin(Time.time, 16f, 1f)
        var r1 = s * 25f
        var r2 = s * 20f

        Draw.z(Layer.effect)
        Draw.color(Color.yellow)

        Lines.circle(unit.x, unit.y, 20f + r1)
        Lines.square(unit.x, unit.y, 20f + r1, Time.time)
        Lines.square(unit.x, unit.y, 20f + r1, -Time.time)

        Tmp.v1.trns(Time.time, r2, r2)

        Fill.circle(unit.x + Tmp.v1.x, unit.y + Tmp.v1.y, 2f + s * 8f)
        Tmp.v1.trns(Time.time, -r2, -r2)
        Fill.circle(unit.x + Tmp.v1.x, unit.y + Tmp.v1.y, 2f + s * 8f)
        Tmp.c1.set(Color.white)
        Tmp.c1.a = 0f
        Fill.light(unit.x, unit.y, 5, 50f - r1, Color.yellow, Tmp.c1)
    }
    
    override fun setStats(){
        super.setStats()
        stats.useCategories = true
        stats.remove(Stat.health)
        stats.add(Stat.health, "${Mathf.round(health * maxLives)} (${health.toInt()} x $maxLives)")
        stats.remove(Stat.itemCapacity)
        stats.add(YellowStats.itemCapacityAlt, "$itemCapacity")
        stats.remove(Stat.weapons)
        stats.add(YellowStats.weaponsAlt){ me: Table ->
            me.background = Styles.grayPanel
            me.add().row()
            for(infel in 1..3) seperator(me, 340f, 4f)
            me.row()
            var ind = 0
            weapons.each{
                val suse = it as NameableWeapon
                me.add(suse.displayName)
                me.button("?"){}.size(35f)
                if(ind ins 3) me.row()
                seperator(me, 340f, 4f)
                me.row()
                ind++
            }
        }
        stats.remove(Stat.range)
        stats.add(YellowStats.rangeAlt, "${(maxRange / Vars.tilesize).toInt()}", StatUnit.blocks)
        
        
        stats.add(YellowStats.maxLives, "$maxLives")
        stats.add(YellowStats.extras, """
        1000-8000 [cyan]shield health[] on first death
        Random chance of teleporting frantically on last life
        Random chance of teleporting frantically AND dropping plasma bombs on last life
        Fourth-wall breaker (Pilot/Human Form)
        """.trimIndent())
        
        stats.add(YellowStats.name, "Nihara")
        stats.add(YellowStats.gender, "Female")
        stats.add(YellowStats.age, "23", YellowStats.yearsOld)
        stats.add(YellowStats.personality, "Kind/Friendly")
        stats.add(YellowStats.headpatRating, "High")
        stats.add(YellowStats.generalAura, "Menacing (First Encounter)")
        stats.add(YellowStats.loveInterest, ".....")
        stats.add(YellowStats.likes, "Comfort, Yellow-colored things, etc...")
        stats.add(YellowStats.dislikes, "Anything explosive, especially Thorium Reactors\n[gray](with the exception of her own weapons in Unit form)[]")
    }
}
