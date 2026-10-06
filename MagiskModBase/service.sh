#!/system/bin/sh
MODDIR=${0%/*} 

# Disattivazione sicura della scala delle gesture laterali (Back Gestures)
# come richiesto dall'utente.
settings put secure back_gesture_inset_scale_left 0.0
settings put secure back_gesture_inset_scale_right 0.0

exit 0
