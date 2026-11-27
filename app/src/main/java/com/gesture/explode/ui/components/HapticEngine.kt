package com.gesture.explode.ui.components

import android.content.Context
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View
import kotlin.math.roundToInt

enum class HapticCue {
    TICK,
    CONFIRM,
    DELETE,
    CLEAR,
    SCROLL_START
}

/**
 * Android 14+ adjustable haptic feedback engine.
 * Prioritizes hardware primitives (VibrationEffect.Composition) for crisp linear-motor scaling,
 * falls back to amplitude-scaled one-shot effects, and finally to View.performHapticFeedback.
 */
object HapticEngine {

    private val touchAttributes: VibrationAttributes by lazy {
        VibrationAttributes.Builder()
            .setUsage(VibrationAttributes.USAGE_TOUCH)
            .build()
    }

    fun trigger(
        context: Context,
        view: View,
        enabled: Boolean,
        strength: Float,
        cue: HapticCue
    ) {
        if (!enabled) return
        val clampedStrength = strength.coerceIn(0.15f, 1.0f)

        val vibrator = getVibrator(context)
        if (vibrator == null || !vibrator.hasVibrator()) {
            fallbackViewHaptic(view, cue)
            return
        }

        try {
            val effect = buildEffect(vibrator, cue, clampedStrength)
            if (effect != null) {
                vibrator.vibrate(effect, touchAttributes)
                return
            }
        } catch (_: Exception) {
            // Fall back safely if OEM restricts vibrator service
        }

        fallbackViewHaptic(view, cue)
    }

    private fun getVibrator(context: Context): Vibrator? {
        val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        return manager?.defaultVibrator
    }

    private fun buildEffect(vibrator: Vibrator, cue: HapticCue, strength: Float): VibrationEffect? {
        val primitiveClickSupported = vibrator.areAllPrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_CLICK)
        val primitiveTickSupported = vibrator.areAllPrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_TICK)
        val primitiveLowTickSupported = vibrator.areAllPrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_LOW_TICK)

        // 1. Best experience: hardware primitives with float intensity scaling (0f..1f)
        when (cue) {
            HapticCue.TICK -> if (primitiveTickSupported) {
                return VibrationEffect.startComposition()
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, (strength * 0.65f).coerceIn(0.1f, 1f))
                    .compose()
            }
            HapticCue.SCROLL_START -> if (primitiveTickSupported) {
                return VibrationEffect.startComposition()
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, (strength * 0.80f).coerceIn(0.15f, 1f))
                    .compose()
            }
            HapticCue.CONFIRM -> if (primitiveClickSupported) {
                return VibrationEffect.startComposition()
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, (strength * 0.95f).coerceIn(0.2f, 1f))
                    .compose()
            }
            HapticCue.DELETE -> if (primitiveLowTickSupported) {
                return VibrationEffect.startComposition()
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_LOW_TICK, (strength * 0.88f).coerceIn(0.18f, 1f))
                    .compose()
            } else if (primitiveClickSupported) {
                return VibrationEffect.startComposition()
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, (strength * 0.75f).coerceIn(0.15f, 1f))
                    .compose()
            }
            HapticCue.CLEAR -> if (primitiveTickSupported && primitiveLowTickSupported) {
                return VibrationEffect.startComposition()
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, (strength * 0.70f).coerceIn(0.15f, 1f))
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_LOW_TICK, (strength * 0.95f).coerceIn(0.2f, 1f), 28)
                    .compose()
            } else if (primitiveClickSupported) {
                return VibrationEffect.startComposition()
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, (strength * 0.95f).coerceIn(0.2f, 1f))
                    .compose()
            }
        }

        // 2. Fallback: amplitude-controlled one-shot pulse
        if (vibrator.hasAmplitudeControl()) {
            val (baseDuration, multiplier) = when (cue) {
                HapticCue.TICK -> 10L to 0.65f
                HapticCue.SCROLL_START -> 12L to 0.78f
                HapticCue.CONFIRM -> 16L to 0.95f
                HapticCue.DELETE -> 18L to 0.88f
                HapticCue.CLEAR -> 22L to 1.0f
            }
            val amplitude = (255f * strength * multiplier).roundToInt().coerceIn(28, 255)
            return VibrationEffect.createOneShot(baseDuration, amplitude)
        }

        return null
    }

    private fun fallbackViewHaptic(view: View, cue: HapticCue) {
        val constant = when (cue) {
            HapticCue.TICK -> HapticFeedbackConstants.CLOCK_TICK
            HapticCue.SCROLL_START -> HapticFeedbackConstants.GESTURE_START
            HapticCue.CONFIRM -> HapticFeedbackConstants.CONFIRM
            HapticCue.DELETE -> HapticFeedbackConstants.GESTURE_END
            HapticCue.CLEAR -> HapticFeedbackConstants.REJECT
        }
        view.performHapticFeedback(constant)
    }
}
