package com.onehumanawa.cfhcore.content.gui.menu.client.loading.scene;

import org.lwjgl.opengl.GL32C;

final class GlSave {
   private int program;
   private int vao;
   private int arrayBuffer;
   private int active;
   private int texture0;
   private int texture1;
   private int drawFbo;
   private int readFbo;
   private int renderbuffer;
   private int srcRgb;
   private int dstRgb;
   private int srcA;
   private int dstA;
   private int equationRgb;
   private int equationA;
   private int depthFunc;
   private int cullMode;
   private int frontFace;
   private int unpackAlignment;
   private int unpackRowLength;
   private int unpackSkipRows;
   private int unpackSkipPixels;
   private int unpackBuffer;
   private boolean blend;
   private boolean depth;
   private boolean cull;
   private boolean scissor;
   private boolean stencil;
   private boolean polygonOffset;
   private boolean multisample;
   private boolean depthMask;
   private final int[] viewport = new int[4];
   private final float[] clear = new float[4];
   private final int[] colourMask = new int[4];
   private final float[] offset = new float[2];
   private final float[] one = new float[1];

   void save() {
      this.program = GL32C.glGetInteger(35725);
      this.vao = GL32C.glGetInteger(34229);
      this.arrayBuffer = GL32C.glGetInteger(34964);
      this.active = GL32C.glGetInteger(34016);
      GL32C.glActiveTexture(33985);
      this.texture1 = GL32C.glGetInteger(32873);
      GL32C.glActiveTexture(33984);
      this.texture0 = GL32C.glGetInteger(32873);
      this.drawFbo = GL32C.glGetInteger(36006);
      this.readFbo = GL32C.glGetInteger(36010);
      this.renderbuffer = GL32C.glGetInteger(36007);
      GL32C.glGetIntegerv(2978, this.viewport);
      this.blend = GL32C.glIsEnabled(3042);
      this.srcRgb = GL32C.glGetInteger(32969);
      this.dstRgb = GL32C.glGetInteger(32968);
      this.srcA = GL32C.glGetInteger(32971);
      this.dstA = GL32C.glGetInteger(32970);
      this.equationRgb = GL32C.glGetInteger(32777);
      this.equationA = GL32C.glGetInteger(34877);
      this.depth = GL32C.glIsEnabled(2929);
      this.depthFunc = GL32C.glGetInteger(2932);
      this.depthMask = GL32C.glGetBoolean(2930);
      this.cull = GL32C.glIsEnabled(2884);
      this.cullMode = GL32C.glGetInteger(2885);
      this.frontFace = GL32C.glGetInteger(2886);
      this.scissor = GL32C.glIsEnabled(3089);
      this.stencil = GL32C.glIsEnabled(2960);
      this.polygonOffset = GL32C.glIsEnabled(32823);
      GL32C.glGetFloatv(32824, this.one);
      this.offset[0] = this.one[0];
      GL32C.glGetFloatv(10752, this.one);
      this.offset[1] = this.one[0];
      this.multisample = GL32C.glIsEnabled(32925);
      GL32C.glGetFloatv(3106, this.clear);
      GL32C.glGetIntegerv(3107, this.colourMask);
      this.unpackAlignment = GL32C.glGetInteger(3317);
      this.unpackRowLength = GL32C.glGetInteger(3314);
      this.unpackSkipRows = GL32C.glGetInteger(3315);
      this.unpackSkipPixels = GL32C.glGetInteger(3316);
      this.unpackBuffer = GL32C.glGetInteger(35055);
   }

   int canvas() {
      return this.drawFbo;
   }

   void restore() {
      GL32C.glUseProgram(this.program);
      GL32C.glBindVertexArray(this.vao);
      GL32C.glBindBuffer(34962, this.arrayBuffer);
      GL32C.glActiveTexture(33985);
      GL32C.glBindTexture(3553, this.texture1);
      GL32C.glActiveTexture(33984);
      GL32C.glBindTexture(3553, this.texture0);
      GL32C.glActiveTexture(this.active);
      GL32C.glBindFramebuffer(36009, this.drawFbo);
      GL32C.glBindFramebuffer(36008, this.readFbo);
      GL32C.glBindRenderbuffer(36161, this.renderbuffer);
      GL32C.glViewport(this.viewport[0], this.viewport[1], this.viewport[2], this.viewport[3]);
      toggle(3042, this.blend);
      GL32C.glBlendFuncSeparate(this.srcRgb, this.dstRgb, this.srcA, this.dstA);
      GL32C.glBlendEquationSeparate(this.equationRgb, this.equationA);
      toggle(2929, this.depth);
      GL32C.glDepthFunc(this.depthFunc);
      GL32C.glDepthMask(this.depthMask);
      toggle(2884, this.cull);
      GL32C.glCullFace(this.cullMode);
      GL32C.glFrontFace(this.frontFace);
      toggle(3089, this.scissor);
      toggle(2960, this.stencil);
      toggle(32823, this.polygonOffset);
      GL32C.glPolygonOffset(this.offset[0], this.offset[1]);
      toggle(32925, this.multisample);
      GL32C.glClearColor(this.clear[0], this.clear[1], this.clear[2], this.clear[3]);
      GL32C.glColorMask(this.colourMask[0] != 0, this.colourMask[1] != 0, this.colourMask[2] != 0, this.colourMask[3] != 0);
      GL32C.glPixelStorei(3317, this.unpackAlignment);
      GL32C.glPixelStorei(3314, this.unpackRowLength);
      GL32C.glPixelStorei(3315, this.unpackSkipRows);
      GL32C.glPixelStorei(3316, this.unpackSkipPixels);
      GL32C.glBindBuffer(35052, this.unpackBuffer);
   }

   static void plainUnpack() {
      GL32C.glBindBuffer(35052, 0);
      GL32C.glPixelStorei(3317, 1);
      GL32C.glPixelStorei(3314, 0);
      GL32C.glPixelStorei(3315, 0);
      GL32C.glPixelStorei(3316, 0);
   }

   private static void toggle(int cap, boolean on) {
      if (on) {
         GL32C.glEnable(cap);
      } else {
         GL32C.glDisable(cap);
      }

   }
}
