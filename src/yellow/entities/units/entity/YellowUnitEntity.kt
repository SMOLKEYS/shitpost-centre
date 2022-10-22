package yellow.entities.units.entity

import arc.math.Mathf
import arc.math.geom.Vec2
import arc.util.io.Reads
import arc.util.io.Writes
import mindustry.Vars
import mindustry.content.UnitTypes
import mindustry.entities.Units
import mindustry.gen.EntityMapping
import mindustry.gen.Groups
import mindustry.gen.Icon
import mindustry.gen.UnitEntity
import yellow.YellowVars.entities
import yellow.entities.units.YellowUnitType
import yellow.game.YellowPermVars

@Suppress("MemberVisibilityCanBePrivate", "unused")
open class YellowUnitEntity: UnitEntity(){

    private var inited = false
    private var franticTeleportTime = 60f
    private val everywhere = Vec2()

    var lives = 0
    var firstDeath = false
    var allowsHealing = false
    var panicMode = false
    var panicModeTypeTwo = false
    var existenceTime = 0f

    
    private fun initVars(){
        inited = true
        lives = type().maxLives
        allowsHealing = Mathf.chance(0.346)
        panicMode = Mathf.chance(0.221)
        panicModeTypeTwo = Mathf.chance(0.124)
        entities.add(this)
    }
    
    private fun invalidateVars(){
        lives = 0
        allowsHealing = false
        panicMode = false
        panicModeTypeTwo = false
        dead = true
        health = 0f
        shield = 0f
        entities.remove(this)
    }
    
    private fun invalidateDeath(){
        lives -= 1
        health = type.health
        dead = false
        elevation = 1f

        if((lives == type().maxLives - 1) && !firstDeath) {
            shield = Mathf.random(1000f, 8000f)
            firstDeath = true
        }
        
        if(isPlayer){
            Vars.ui.hudfrag.showToast(Icon.warning, "$lives left!")
        }
        
        //sorry, but yellow ain't going down to the void
        if(outOfWorldBounds()){
            if(team.data().cores.isEmpty){
                x = Mathf.random(Vars.world.width()) * 8f
                y = Mathf.random(Vars.world.height()) * 8f
            }else{
                val core = team.data().cores[0]
                x = core.x
                y = core.y
            }
        }else{
            x += Mathf.range(25f * 8f)
            y += Mathf.range(25f * 8f)
        }
    }
    
    private fun lifetimeCompare(runnable: () -> Unit){
        Groups.unit.each{
            if(it != this && it is YellowUnitEntity){
                if(it.existenceTime < existenceTime) runnable()
            }
        }
    }
    
    private fun destroyFull(){
        invalidateVars()
        super.destroy()
    }
    
    private fun removeFull(){
        invalidateVars()
        super.remove()
    }
    
    fun outOfWorldBounds(): Boolean{
        return x > Vars.world.width() * 8f || x < 0f || y > Vars.world.height() * 8f || y < 0f
    }


    fun forceKill() {
        while(lives > 0) {
            kill()
        }
    }
    
    //0 - dead/invalid + not in list, should not be in list anyway
    //1 - alive but not in list
    //2 - alive and in list
    fun listed(): Int{
        var bus = 0
        if(!isValid || dead || (type().maxLives <= 0 && health <= 0)) bus = 0
        entities.each{
            bus = if(!dead && isValid && it == this) 2 else 1
        }
        return bus
    }

    override fun type(): YellowUnitType {
        return type as YellowUnitType
    }

    override fun kill() {
        destroy() //just call destroy(), no point in waiting
    }
    

    override fun destroy() {
        lifetimeCompare{ destroyFull() }
        
        if(lives > 1) {
            invalidateDeath()
            return
        }
        
        destroyFull()
    }

    override fun remove() {
        lifetimeCompare{ removeFull() }
        
        if(!YellowPermVars.removeAllowed && lives > 1){
            return
        }


        removeFull()
    }

    override fun update() {
        super.update()

        if(!inited) {
            initVars()
        }

        spawnedByCore = true
        existenceTime++

        if(team.data().countType(type) > 1) {
            YellowPermVars.removeAllowed = true
            remove()
        } else {
            YellowPermVars.removeAllowed = false
        }
        
        if(listed() == 1) entities.add(this)
        
        //heal surrounding units; normal units gain 10 health, player units gain either no health or a third of their current health
        //very picky woman i must say :trollar:
        if(allowsHealing){
            Units.nearby(x, y, 15f*8f, 15f*8f){a: mindustry.gen.Unit ->
                if(a.team == team){
                    if(!a.isPlayer){
                        if(Mathf.chanceDelta(0.09)){
                            a.heal(10f)
                        }
                    }else{
                        if(Mathf.chanceDelta(0.14)){
                            a.heal(Mathf.random() * a.health.div(3f))
                        }
                    }
                }
            }
        }
        
        
        //teleport everywhere and start firing all weapons on last life for a second if panic mode is enabled
        if(panicMode && lives == 1 && franticTeleportTime > 0f){
            everywhere.set(Mathf.random(Vars.world.width()) * 8f, Mathf.random(Vars.world.height()) * 8f)
            x = everywhere.x
            y = everywhere.y

            val mountus = mounts.random()

            mountus.shoot = true
            mountus.weapon.update(this, mountus)
            
            //if type two panic mode is enabled, start dropping quad bombs
            if(panicModeTypeTwo){
                UnitTypes.quad.weapons[0].bullet.create(this, x, y, 0f)
            }
            
            franticTeleportTime--
        }
    }
    
    override fun toString(): String{
        return if(isValid) "YellowUnitEntity#$id" else "(invalid) YellowUnitEntity#$id"
    }


    override fun write(write: Writes) {
        super.write(write)
        write.bool(inited)
        write.bool(firstDeath)
        write.bool(allowsHealing)
        write.bool(panicMode)
        write.bool(panicModeTypeTwo)
        write.i(lives)
        write.f(franticTeleportTime)
        /*
        mounts.forEach{
            val wts = it as DisableableWeaponMount
            wts.write(write)
        }
        */
    }

    override fun read(read: Reads) {
        super.read(read)
        inited = read.bool()
        firstDeath = read.bool()
        allowsHealing = read.bool()
        panicMode = read.bool()
        panicModeTypeTwo = read.bool()
        lives = read.i()
        franticTeleportTime = read.f()
        /*
        mounts.forEach{
            val rts = it as DisableableWeaponMount
            rts.read(read)
        }
        */
    }

    override fun classId() = mappingId

    companion object {
        val mappingId = EntityMapping.register("ion-yellow-unit", ::YellowUnitEntity)
    }
}
