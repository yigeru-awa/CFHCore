package com.onehumanawa.cfhcore.content.gui.menu.client.infohud;

import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.WGL;
import org.lwjgl.opengl.WGLAMDGPUAssociation;
import org.lwjgl.opengl.WGLCapabilities;
import org.lwjgl.system.Platform;

final class AmdCard {
   private AmdCard() {
   }

   static long totalKb() {
      if (Platform.get() != Platform.WINDOWS) {
         return -1L;
      } else {
         WGLCapabilities wgl = GL.getCapabilitiesWGL();
         if (wgl != null && wgl.WGL_AMD_gpu_association) {
            int[] ids = new int[16];
            int count = Math.min(ids.length, WGLAMDGPUAssociation.wglGetGPUIDsAMD(ids));
            if (count <= 0) {
               return -1L;
            } else {
               int id = WGLAMDGPUAssociation.wglGetContextGPUIDAMD(WGL.wglGetCurrentContext());
               if (id == 0) {
                  id = ids[0];
               }

               int[] megabytes = new int[1];
               int written = WGLAMDGPUAssociation.wglGetGPUInfoAMD(id, 8611, 5125, megabytes);
               return written > 0 && megabytes[0] > 0 ? (long)megabytes[0] * 1024L : -1L;
            }
         } else {
            return -1L;
         }
      }
   }
}
