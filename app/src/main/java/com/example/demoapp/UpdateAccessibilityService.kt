package com.example.demoapp

import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.util.Log

class UpdateAccessibilityService : android.accessibilityservice.AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || 
            event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            
            val rootNode = rootInActiveWindow ?: return
            
            // Search for "Install" or "Update" text in the system dialog
            findAndClickButton(rootNode, "INSTALL")
            findAndClickButton(rootNode, "UPDATE")
            findAndClickButton(rootNode, "Install")
            findAndClickButton(rootNode, "Update")
            
            // Also handle the "Done" or "Open" button at the end if needed
            findAndClickButton(rootNode, "DONE")
            findAndClickButton(rootNode, "Done")
        }
    }

    private fun findAndClickButton(node: AccessibilityNodeInfo, text: String): Boolean {
        val list = node.findAccessibilityNodeInfosByText(text)
        for (item in list) {
            if (item.isClickable && item.isEnabled) {
                item.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                Log.d("UpdateAccessibility", "Clicked $text button")
                return true
            }
        }
        
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (findAndClickButton(child, text)) return true
        }
        return false
    }

    override fun onInterrupt() {
        Log.e("UpdateAccessibility", "Service Interrupted")
    }
}
