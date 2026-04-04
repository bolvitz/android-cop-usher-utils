package com.eventmonitor.shared.platform

import platform.UIKit.UIImpactFeedbackGenerator
import platform.UIKit.UIImpactFeedbackStyle
import platform.UIKit.UINotificationFeedbackGenerator
import platform.UIKit.UINotificationFeedbackType

actual class HapticFeedback {
    private val impactLight = UIImpactFeedbackGenerator(UIImpactFeedbackStyle.UIImpactFeedbackStyleLight)
    private val impactMedium = UIImpactFeedbackGenerator(UIImpactFeedbackStyle.UIImpactFeedbackStyleMedium)
    private val impactHeavy = UIImpactFeedbackGenerator(UIImpactFeedbackStyle.UIImpactFeedbackStyleHeavy)
    private val notificationGenerator = UINotificationFeedbackGenerator()

    actual fun perform(type: HapticFeedbackType) {
        when (type) {
            HapticFeedbackType.LIGHT -> {
                impactLight.prepare()
                impactLight.impactOccurred()
            }
            HapticFeedbackType.MEDIUM -> {
                impactMedium.prepare()
                impactMedium.impactOccurred()
            }
            HapticFeedbackType.HEAVY -> {
                impactHeavy.prepare()
                impactHeavy.impactOccurred()
            }
            HapticFeedbackType.SUCCESS -> {
                notificationGenerator.prepare()
                notificationGenerator.notificationOccurred(UINotificationFeedbackType.UINotificationFeedbackTypeSuccess)
            }
            HapticFeedbackType.WARNING -> {
                notificationGenerator.prepare()
                notificationGenerator.notificationOccurred(UINotificationFeedbackType.UINotificationFeedbackTypeWarning)
            }
            HapticFeedbackType.ERROR -> {
                notificationGenerator.prepare()
                notificationGenerator.notificationOccurred(UINotificationFeedbackType.UINotificationFeedbackTypeError)
            }
        }
    }
}
