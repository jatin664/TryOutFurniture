package com.app.tryoutfurniture

import com.google.ar.sceneform.ux.ArFragment

class CustomArFragment : ArFragment() {

    override fun getAdditionalPermissions(): Array<String> {
        //get previously added permissions
        val additionalPermissions = super.getAdditionalPermissions()

        val permissionsLength = additionalPermissions.size

        val permissions = Array(permissionsLength + 1) {
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        }

        if(permissionsLength > 0){
            System.arraycopy(additionalPermissions,0,permissions,1,permissionsLength)
        }

        return permissions

    }
}