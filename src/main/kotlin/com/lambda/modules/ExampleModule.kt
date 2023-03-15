package com.lambda.modules

import com.lambda.ExamplePlugin
import com.lambda.client.event.events.RenderWorldEvent
import com.lambda.client.module.Category
import com.lambda.client.plugin.api.PluginModule
import com.lambda.client.util.TickTimer
import com.lambda.client.util.TimeUnit
import com.lambda.client.util.color.ColorHolder
import com.lambda.client.util.graphics.ESPRenderer
import com.lambda.client.util.graphics.GeometryMasks
import com.lambda.client.util.math.VectorUtils.toBlockPos
import com.lambda.client.util.math.VectorUtils.toVec3d
import com.lambda.client.util.threads.defaultScope
import com.lambda.client.util.threads.safeListener
import io.netty.util.internal.ConcurrentSet
import kotlinx.coroutines.launch
import net.minecraft.tileentity.TileEntitySign
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import net.minecraft.util.text.ITextComponent
import net.minecraft.util.text.TextComponentString
import java.lang.Exception
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.Consumer
import java.util.stream.Collectors
import kotlin.math.roundToInt


internal object ExampleModule : PluginModule(
    name = "OldSigns",
    category = Category.MISC,
    description = "",
    pluginMain = ExamplePlugin
) {

    private val delay by setting("Update Delay MS", 800, 500..10000, 5)
    private val range by setting("Range", 100, 30..250, 5)
    private val color by setting("Color", ColorHolder(r = 30, g = 30, b = 30, a = 50), false)

    private var oldSignBlocks: ConcurrentSet<BlockPos> = ConcurrentSet<BlockPos>()
    private var updateTimer: TickTimer = TickTimer(TimeUnit.MILLISECONDS)

    private val renderer = ESPRenderer();

    init {

        safeListener<RenderWorldEvent> {
            if (mc.currentScreen != null) {
                return@safeListener;
            }
            defaultScope.launch {
                updateOldSignBlocks()
            }

            renderer.render(true)
        }
    }


    private fun updateOldSignBlocks() {
        renderer.aFilled = 30
        renderer.aOutline = 30
        oldSignBlocks.clear()
        for (pos in getBlocksInRadius(range)!!) {
            if (isOldSignText(pos)) {
                val side = GeometryMasks.Quad.ALL
                renderer.add(Triple(AxisAlignedBB(pos), color, side))
            }
        }
    }

    private fun isOldSignText(pos: BlockPos?): Boolean {
        // Explanation: Old signs on 2b2t (pre-2015 <1.9 ?) have older style NBT text tags.
        // we can tell them apart by checking if there are siblings in the tag. Old signs won't have siblings.
        val sign: TileEntitySign?
        if (mc.player.world.getTileEntity(pos) is TileEntitySign) {
            sign = mc.player.world.getTileEntity(pos) as TileEntitySign?
            assert(sign != null)
            val signTextComponents = Arrays.stream(sign!!.signText).filter { component: ITextComponent? -> component is TextComponentString }
                .map { component: ITextComponent -> component as TextComponentString }
                .collect(Collectors.toList())
            val empty = AtomicBoolean(false)

            //avoid blank signs
            signTextComponents.forEach(Consumer { t: TextComponentString ->
                if (t.text.isEmpty()) {
                    empty.set(true)
                }
            })
            return if (empty.get()) {
                false
            } else signTextComponents.stream()
                .allMatch { component: TextComponentString -> component.siblings.isEmpty() }
        }
        return false
    }


    private fun getBlocksInRadius(range: Int): List<BlockPos>? {
        val posses: MutableList<BlockPos> = ArrayList()
        val xRange = range.toDouble().roundToInt().toFloat()
        val yRange = range.toDouble().roundToInt().toFloat()
        val zRange = range.toDouble().roundToInt().toFloat()
        var x = -xRange
        while (x <= xRange) {
            var y = -yRange
            while (y <= yRange) {
                var z = -zRange
                while (z <= zRange) {
                    val position: BlockPos = mc.player.position.toVec3d().add(x.toDouble(), y.toDouble(), z.toDouble()).toBlockPos()
                    if (mc.player.getDistance(position.x + 0.5, (position.y + 1).toDouble(), position.z + 0.5) <= range) {
                        posses.add(position)
                    }
                    z++
                }
                y++
            }
            x++
        }
        return posses
    }
}