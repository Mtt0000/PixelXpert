PKGNAME="sh.siava.pixelxpert"
PKGPATH="/system/priv-app/PixelXpert/PixelXpert.apk"

testKernelSU()
{
	if [[ $(ksud -V 2>&1 | grep "not found" | wc -c) -eq 0 ]]; then #KSU installed
    	if [[ $(pm list packages | grep $PKGNAME | wc -c) -eq 0 ]]; then #PixelXpert NOT installed yet
    		ui_print ''
    		ui_print '*******************************'
    		ui_print 'KernelSU binaries found!'
    		ui_print ''
    		ui_print '                CAUTION!:'
    		ui_print 'Before installation, you MUST disable'
    		ui_print '"Umount modules by default"'
    		ui_print 'Otherwise, your device will fall into BOOTLOOP!'
    		ui_print ''
    		ui_print 'Do you wish to continue?'
    		ui_print 'Volume Up: Continue'
    		ui_print 'Volume Down: Abort'
    		if [[ "$(getevent -l | grep -m 1 KEY_VOLUME)" == *"VOLUMEDOWN"* ]]; then
    			abort 'Installation cancelled'
    		fi;
    	fi;
    fi;
}

testKernelSU

ui_print ''
ui_print ''
ui_print 'PixelXpert Modded Version'
ui_print 'Applying safe installations...'
ui_print ''

set_perm $MODPATH/service.sh 0 0 0755

ui_print 'Installation Complete!'
ui_print 'Please Reboot your device to activate.'
ui_print ''
ui_print 'Don\'t forget to manually enable the module in LSPosed.'
ui_print ''
ui_print '  **********************'
ui_print '  * Brought to you by: *'
ui_print '  * PixelXpert team    *'
ui_print '  * Modded for Android17*'
ui_print '  **********************'
ui_print ''
