JNI_LOCAL_PATH := $(call my-dir)

AE_BRIDGE_INCLUDES := $(JNI_LOCAL_PATH)/ae-bridge/
M64P_API_INCLUDES := $(JNI_LOCAL_PATH)/mupen64plus-core/src/api/
SDL_INCLUDES := $(JNI_LOCAL_PATH)/SDL2/include/
PNG_INCLUDES := $(JNI_LOCAL_PATH)/png/include/
SAMPLERATE_INCLUDES := $(JNI_LOCAL_PATH)/libsamplerate/

COMMON_CFLAGS :=                    \
    -O3                             \
    -ffast-math                     \
    -fno-strict-aliasing            \
    -fomit-frame-pointer            \
    -frename-registers              \
    -fsingle-precision-constant     \
    -fvisibility=hidden             \

COMMON_CPPFLAGS :=                  \
    -fvisibility-inlines-hidden     \

include $(call all-subdir-makefiles)

LOCAL_PATH := $(JNI_LOCAL_PATH)


########################
# mupen64plus-audio-sdl
########################
include $(CLEAR_VARS)
SRCDIR := mupen64plus-audio-sdl/src

LOCAL_MODULE := mupen64plus-audio-sdl
LOCAL_SHARED_LIBRARIES := SDL2
LOCAL_STATIC_LIBRARIES := samplerate
#LOCAL_ARM_MODE := arm

LOCAL_C_INCLUDES :=         \
    $(M64P_API_INCLUDES)    \
    $(SDL_INCLUDES)         \
    $(SAMPLERATE_INCLUDES)  \

LOCAL_SRC_FILES :=                      \
    $(SRCDIR)/main.c                    \
    $(SRCDIR)/volume.c                  \
    $(SRCDIR)/osal_dynamiclib_unix.c    \

LOCAL_CFLAGS :=         \
    $(COMMON_CFLAGS)    \
    -DUSE_SRC           \

include $(BUILD_SHARED_LIBRARY)


###################
# mupen64plus-core
###################
include $(CLEAR_VARS)
SRCDIR := ./mupen64plus-core/src

LOCAL_MODULE := mupen64plus-core
LOCAL_SHARED_LIBRARIES := SDL2
LOCAL_STATIC_LIBRARIES := png
LOCAL_ARM_MODE := arm

LOCAL_C_INCLUDES :=         \
    $(LOCAL_PATH)/$(SRCDIR) \
    $(PNG_INCLUDES)         \
    $(SDL_INCLUDES)         \

LOCAL_SRC_FILES :=                              \
    $(SRCDIR)/ai/ai_controller.c                \
    $(SRCDIR)/api/callbacks.c                   \
    $(SRCDIR)/api/common.c                      \
    $(SRCDIR)/api/config.c                      \
    $(SRCDIR)/api/debugger.c                    \
    $(SRCDIR)/api/frontend.c                    \
    $(SRCDIR)/api/vidext.c                      \
    $(SRCDIR)/main/cheat.c                      \
    $(SRCDIR)/main/eep_file.c                   \
    $(SRCDIR)/main/eventloop.c                  \
    $(SRCDIR)/main/fla_file.c                   \
    $(SRCDIR)/main/main.c                       \
    $(SRCDIR)/main/md5.c                        \
    $(SRCDIR)/main/mpk_file.c                   \
    $(SRCDIR)/main/profile.c                    \
    $(SRCDIR)/main/rom.c                        \
    $(SRCDIR)/main/savestates.c                 \
    $(SRCDIR)/main/sdl_key_converter.c          \
    $(SRCDIR)/main/sra_file.c                   \
    $(SRCDIR)/main/util.c                       \
    $(SRCDIR)/main/zip/ioapi.c                  \
    $(SRCDIR)/main/zip/unzip.c                  \
    $(SRCDIR)/main/zip/zip.c                    \
    $(SRCDIR)/memory/memory.c                   \
    $(SRCDIR)/osal/dynamiclib_unix.c            \
    $(SRCDIR)/osal/files_unix.c                 \
    $(SRCDIR)/osd/screenshot.cpp                \
    $(SRCDIR)/pi/cart_rom.c                     \
    $(SRCDIR)/pi/flashram.c                     \
    $(SRCDIR)/pi/pi_controller.c                \
    $(SRCDIR)/pi/sram.c                         \
    $(SRCDIR)/plugin/dummy_audio.c              \
    $(SRCDIR)/plugin/dummy_input.c              \
    $(SRCDIR)/plugin/dummy_rsp.c                \
    $(SRCDIR)/plugin/dummy_video.c              \
    $(SRCDIR)/plugin/emulate_game_controller_via_input_plugin.c \
    $(SRCDIR)/plugin/emulate_speaker_via_audio_plugin.c \
    $(SRCDIR)/plugin/get_time_using_C_localtime.c \
    $(SRCDIR)/plugin/plugin.c                   \
    $(SRCDIR)/plugin/rumble_via_input_plugin.c  \
    $(SRCDIR)/r4300/cached_interp.c             \
    $(SRCDIR)/r4300/cp0.c                       \
    $(SRCDIR)/r4300/cp1.c                       \
    $(SRCDIR)/r4300/empty_dynarec.c             \
    $(SRCDIR)/r4300/exception.c                 \
    $(SRCDIR)/r4300/instr_counters.c            \
    $(SRCDIR)/r4300/interupt.c                  \
    $(SRCDIR)/r4300/mi_controller.c             \
    $(SRCDIR)/r4300/pure_interp.c               \
    $(SRCDIR)/r4300/r4300.c                     \
    $(SRCDIR)/r4300/r4300_core.c                \
    $(SRCDIR)/r4300/recomp.c                    \
    $(SRCDIR)/r4300/reset.c                     \
    $(SRCDIR)/r4300/tlb.c                       \
    $(SRCDIR)/r4300/new_dynarec/new_dynarec.c   \
    $(SRCDIR)/rdp/fb.c                          \
    $(SRCDIR)/rdp/rdp_core.c                    \
    $(SRCDIR)/ri/rdram.c                        \
    $(SRCDIR)/ri/rdram_detection_hack.c         \
    $(SRCDIR)/ri/ri_controller.c                \
    $(SRCDIR)/rsp/rsp_core.c                    \
    $(SRCDIR)/si/af_rtc.c                       \
    $(SRCDIR)/si/cic.c                          \
    $(SRCDIR)/si/eeprom.c                       \
    $(SRCDIR)/si/game_controller.c              \
    $(SRCDIR)/si/mempak.c                       \
    $(SRCDIR)/si/n64_cic_nus_6105.c             \
    $(SRCDIR)/si/pif.c                          \
    $(SRCDIR)/si/rumblepak.c                    \
    $(SRCDIR)/si/si_controller.c                \
    $(SRCDIR)/vi/vi_controller.c                \
    #$(SRCDIR)/debugger/dbg_breakpoints.c        \
    #$(SRCDIR)/debugger/dbg_debugger.c           \
    #$(SRCDIR)/debugger/dbg_decoder.c            \
    #$(SRCDIR)/debugger/dbg_memory.c             \

LOCAL_CFLAGS :=         \
    $(COMMON_CFLAGS)    \
    -DANDROID           \
    -DIOAPI_NO_64       \
    -DNOCRYPT           \
    -DNOUNCRYPT         \
    -DUSE_GLES=1        \

LOCAL_LDFLAGS :=                                                    \
    -Wl,-Bsymbolic                                                  \
    -Wl,-export-dynamic                                             \
    -Wl,-version-script,$(LOCAL_PATH)/$(SRCDIR)/api/api_export.ver  \

LOCAL_LDLIBS := -lz

ifeq ($(TARGET_ARCH_ABI), armeabi-v7a)
    # Use for ARM7a:
    LOCAL_SRC_FILES += $(SRCDIR)/r4300/new_dynarec/arm/linkage_arm.S
    LOCAL_SRC_FILES += $(SRCDIR)/r4300/new_dynarec/arm/arm_cpu_features.c
    LOCAL_CFLAGS += -DDYNAREC
    LOCAL_CFLAGS += -DNEW_DYNAREC=3
    LOCAL_CFLAGS += -mfloat-abi=softfp
    LOCAL_CFLAGS += -mfpu=vfp

else ifeq ($(TARGET_ARCH_ABI), armeabi)
    # Use for pre-ARM7a:
    LOCAL_SRC_FILES += $(SRCDIR)/r4300/new_dynarec/arm/linkage_arm.S
    LOCAL_SRC_FILES += $(SRCDIR)/r4300/new_dynarec/arm/arm_cpu_features.c
    LOCAL_CFLAGS += -DARMv5_ONLY
    LOCAL_CFLAGS += -DDYNAREC
    LOCAL_CFLAGS += -DNEW_DYNAREC=3

else ifeq ($(TARGET_ARCH_ABI), x86)
    # Use for x86:
    LOCAL_ASMFLAGS = -d ELF_TYPE
    LOCAL_SRC_FILES += $(SRCDIR)/r4300/new_dynarec/x86/linkage_x86.asm
    LOCAL_CFLAGS += -DDYNAREC
    LOCAL_CFLAGS += -DNEW_DYNAREC=1

else ifeq ($(TARGET_ARCH_ABI), mips)
    # Use for MIPS:
    #TODO: Possible to port dynarec from Daedalus? 

else
    # Any other architectures that Android could be running on?

endif

include $(BUILD_SHARED_LIBRARY)


######################
# mupen64plus-rsp-hle
######################
include $(CLEAR_VARS)
SRCDIR := ./mupen64plus-rsp-hle/src

LOCAL_MODULE := mupen64plus-rsp-hle
LOCAL_ARM_MODE := arm

LOCAL_C_INCLUDES := $(M64P_API_INCLUDES)

LOCAL_SRC_FILES :=           \
    $(SRCDIR)/alist.c        \
    $(SRCDIR)/alist_audio.c  \
    $(SRCDIR)/alist_naudio.c \
    $(SRCDIR)/alist_nead.c   \
    $(SRCDIR)/audio.c        \
    $(SRCDIR)/cicx105.c      \
    $(SRCDIR)/hle.c          \
    $(SRCDIR)/jpeg.c         \
    $(SRCDIR)/memory.c       \
    $(SRCDIR)/mp3.c          \
    $(SRCDIR)/musyx.c        \
    $(SRCDIR)/plugin.c       \

LOCAL_CFLAGS := $(COMMON_CFLAGS)

LOCAL_CPPFLAGS := $(COMMON_CPPFLAGS)

LOCAL_LDFLAGS := -Wl,-version-script,$(LOCAL_PATH)/$(SRCDIR)/rsp_api_export.ver

include $(BUILD_SHARED_LIBRARY)


#########################
# mupen64plus-ui-console
#########################
include $(CLEAR_VARS)
SRCDIR := ./mupen64plus-ui-console/src

LOCAL_MODULE := mupen64plus-ui-console
LOCAL_SHARED_LIBRARIES := ae-imports
#LOCAL_ARM_MODE := arm

LOCAL_C_INCLUDES :=         \
    $(LOCAL_PATH)/$(SRCDIR) \
    $(M64P_API_INCLUDES)    \
    $(SDL_INCLUDES)         \
    $(AE_BRIDGE_INCLUDES)   \

LOCAL_SRC_FILES :=                      \
    $(SRCDIR)/cheat.c                   \
    $(SRCDIR)/compare_core.c            \
    $(SRCDIR)/core_interface.c          \
    $(SRCDIR)/main.c                    \
    $(SRCDIR)/osal_dynamiclib_unix.c    \
    $(SRCDIR)/osal_files_unix.c         \
    $(SRCDIR)/plugin.c                  \

LOCAL_CFLAGS :=                                 \
    $(COMMON_CFLAGS)                            \
    -DANDROID                                   \
    -DNO_ASM                                    \
    -DCALLBACK_HEADER=ae_imports.h              \
    -DCALLBACK_FUNC=Android_JNI_StateCallback   \

LOCAL_CPPFLAGS := $(COMMON_CPPFLAGS)

LOCAL_LDLIBS := -llog

include $(BUILD_SHARED_LIBRARY)


###############################
# mupen64plus-video-glide64mk2
###############################
include $(CLEAR_VARS)
SRCDIR := ./mupen64plus-video-glide64mk2/src

LOCAL_MODULE := mupen64plus-video-glide64mk2
LOCAL_SHARED_LIBRARIES := SDL2
LOCAL_STATIC_LIBRARIES := png
LOCAL_ARM_MODE := arm

LOCAL_C_INCLUDES :=                             \
    $(LOCAL_PATH)/$(SRCDIR)/Glitch64/inc        \
    $(M64P_API_INCLUDES)                        \
    $(PNG_INCLUDES)                             \
    $(SDL_INCLUDES)                             \

LOCAL_SRC_FILES :=                              \
    $(SRCDIR)/Glitch64/OGLEScombiner.cpp        \
    $(SRCDIR)/Glitch64/OGLESgeometry.cpp        \
    $(SRCDIR)/Glitch64/OGLESglitchmain.cpp      \
    $(SRCDIR)/Glitch64/OGLEStextures.cpp        \
    $(SRCDIR)/Glide64/osal_dynamiclib_unix.c    \
    $(SRCDIR)/Glide64/3dmath.cpp                \
    $(SRCDIR)/Glide64/Combine.cpp               \
    $(SRCDIR)/Glide64/Config.cpp                \
    $(SRCDIR)/Glide64/CRC.cpp                   \
    $(SRCDIR)/Glide64/Debugger.cpp              \
    $(SRCDIR)/Glide64/DepthBufferRender.cpp     \
    $(SRCDIR)/Glide64/FBtoScreen.cpp            \
    $(SRCDIR)/Glide64/FrameSkipper.cpp          \
    $(SRCDIR)/Glide64/Ini.cpp                   \
    $(SRCDIR)/Glide64/Keys.cpp                  \
    $(SRCDIR)/Glide64/Main.cpp                  \
    $(SRCDIR)/Glide64/rdp.cpp                   \
    $(SRCDIR)/Glide64/TexBuffer.cpp             \
    $(SRCDIR)/Glide64/TexCache.cpp              \
    $(SRCDIR)/Glide64/Util.cpp                  \
#    $(SRCDIR)/GlideHQ/Ext_TxFilter.cpp          \
#    $(SRCDIR)/GlideHQ/TxFilterExport.cpp        \
#    $(SRCDIR)/GlideHQ/TxFilter.cpp              \
#    $(SRCDIR)/GlideHQ/TxCache.cpp               \
#    $(SRCDIR)/GlideHQ/TxTexCache.cpp            \
#    $(SRCDIR)/GlideHQ/TxHiResCache.cpp          \
#    $(SRCDIR)/GlideHQ/TxQuantize.cpp            \
#    $(SRCDIR)/GlideHQ/TxUtil.cpp                \
#    $(SRCDIR)/GlideHQ/TextureFilters.cpp        \
#    $(SRCDIR)/GlideHQ/TextureFilters_2xsai.cpp  \
#    $(SRCDIR)/GlideHQ/TextureFilters_hq2x.cpp   \
#    $(SRCDIR)/GlideHQ/TextureFilters_hq4x.cpp   \
#    $(SRCDIR)/GlideHQ/TxImage.cpp               \
#    $(SRCDIR)/GlideHQ/TxReSample.cpp            \
#    $(SRCDIR)/GlideHQ/TxDbg.cpp                 \
#    $(SRCDIR)/GlideHQ/tc-1.1+/fxt1.c            \
#    $(SRCDIR)/GlideHQ/tc-1.1+/dxtn.c            \
#    $(SRCDIR)/GlideHQ/tc-1.1+/wrapper.c         \
#    $(SRCDIR)/GlideHQ/tc-1.1+/texstore.c        \

LOCAL_CFLAGS :=         \
    $(COMMON_CFLAGS)    \
    -DANDROID           \
    -DUSE_FRAMESKIPPER  \
    -DNOSSE             \
    -DNO_ASM            \
    -DUSE_GLES          \
    -fsigned-char       \
    
LOCAL_CPPFLAGS := $(COMMON_CPPFLAGS)
    
LOCAL_CPP_FEATURES := exceptions

LOCAL_LDFLAGS := -Wl,-version-script,$(LOCAL_PATH)/$(SRCDIR)/video_api_export.ver

LOCAL_LDLIBS :=         \
    -ldl                \
    -lGLESv2            \
    -llog               \
    -lz                 \

ifeq ($(TARGET_ARCH_ABI), armeabi-v7a)
    # Use for ARM7a:
    LOCAL_CFLAGS += -mfpu=vfp
    LOCAL_CFLAGS += -mfloat-abi=softfp
    
else ifeq ($(TARGET_ARCH_ABI), armeabi)
    # Use for pre-ARM7a:
    
else ifeq ($(TARGET_ARCH_ABI), x86)
    # TODO: set the proper flags here
    
else
    # Any other architectures that Android could be running on?
    
endif

include $(BUILD_SHARED_LIBRARY)


#############################
# mupen64plus-video-gliden64
#############################
include $(CLEAR_VARS)
SRCDIR := ./mupen64plus-video-gliden64/src

MY_LOCAL_MODULE := mupen64plus-video-gliden64
MY_LOCAL_SHARED_LIBRARIES := ae-imports SDL2
MY_LOCAL_STATIC_LIBRARIES := cpufeatures
MY_LOCAL_ARM_MODE := arm

MY_LOCAL_C_INCLUDES :=                          \
    $(LOCAL_PATH)/$(SRCDIR)                     \
    $(LOCAL_PATH)/$(SRCDIR)/inc                 \
    $(M64P_API_INCLUDES)                        \
    $(SDL_INCLUDES)                             \
    $(AE_BRIDGE_INCLUDES)                       \

MY_LOCAL_SRC_FILES :=                               \
    $(SRCDIR)/3DMath.cpp                            \
    $(SRCDIR)/Combiner.cpp                          \
    $(SRCDIR)/CommonPluginAPI.cpp                   \
    $(SRCDIR)/Config.cpp                            \
    $(SRCDIR)/CRC.cpp                               \
    $(SRCDIR)/DepthBuffer.cpp                       \
    $(SRCDIR)/F3D.cpp                               \
    $(SRCDIR)/F3DDKR.cpp                            \
    $(SRCDIR)/F3DEX2CBFD.cpp                        \
    $(SRCDIR)/F3DEX2.cpp                            \
    $(SRCDIR)/F3DEX.cpp                             \
    $(SRCDIR)/F3DPD.cpp                             \
    $(SRCDIR)/F3DSWSE.cpp                           \
    $(SRCDIR)/F3DWRUS.cpp                           \
    $(SRCDIR)/FrameBuffer.cpp                       \
    $(SRCDIR)/GBI.cpp                               \
    $(SRCDIR)/gDP.cpp                               \
    $(SRCDIR)/GLideN64.cpp                          \
    $(SRCDIR)/glState.cpp                           \
    $(SRCDIR)/gSP.cpp                               \
    $(SRCDIR)/Keys.cpp                              \
    $(SRCDIR)/L3D.cpp                               \
    $(SRCDIR)/L3DEX2.cpp                            \
    $(SRCDIR)/L3DEX.cpp                             \
    $(SRCDIR)/MupenPlusPluginAPI.cpp                \
    $(SRCDIR)/N64.cpp                               \
    $(SRCDIR)/OpenGL.cpp                            \
    $(SRCDIR)/PostProcessor.cpp                     \
    $(SRCDIR)/RDP.cpp                               \
    $(SRCDIR)/RSP.cpp                               \
    $(SRCDIR)/S2DEX2.cpp                            \
    $(SRCDIR)/S2DEX.cpp                             \
    $(SRCDIR)/Textures.cpp                          \
    $(SRCDIR)/Turbo3D.cpp                           \
    $(SRCDIR)/VI.cpp                                \
    $(SRCDIR)/ZSort.cpp                             \
    $(SRCDIR)/ShaderUtils.cpp                       \
    $(SRCDIR)/common/CommonAPIImpl_common.cpp       \
    $(SRCDIR)/mupenplus/CommonAPIImpl_mupenplus.cpp \
    $(SRCDIR)/mupenplus/Config_mupenplus.cpp        \
    $(SRCDIR)/mupenplus/MupenPlusAPIImpl.cpp        \
    $(SRCDIR)/mupenplus/OpenGL_mupenplus.cpp        \
    $(SRCDIR)/TextDrawerStub.cpp                    \
    $(SRCDIR)/TxFilterStub.cpp                      \

MY_LOCAL_CFLAGS :=      \
    $(COMMON_CFLAGS)    \
    -g                  \
    -DANDROID           \
    -DUSE_SDL           \
    -DMUPENPLUSAPI      \
    -fsigned-char       \
    #-DSDL_NO_COMPAT     \

MY_LOCAL_CPPFLAGS := $(COMMON_CPPFLAGS) -std=c++11 -g

MY_LOCAL_LDFLAGS := -Wl,-version-script,$(LOCAL_PATH)/$(SRCDIR)/mupenplus/video_api_export.ver

MY_LOCAL_LDLIBS := -llog

ifeq ($(TARGET_ARCH_ABI), armeabi-v7a)
    # Use for ARM7a:
    #MY_LOCAL_SRC_FILES += gSPNeon.cpp.neon
    #MY_LOCAL_SRC_FILES += 3DMathNeon.cpp.neon 
    MY_LOCAL_CFLAGS += -DARM_ASM
    MY_LOCAL_CFLAGS += -D__NEON_OPT
    
else ifeq ($(TARGET_ARCH_ABI), armeabi)
    # Use for pre-ARM7a:
    
else ifeq ($(TARGET_ARCH_ABI), x86)
    # TODO: set the proper flags here
    
else
    # Any other architectures that Android could be running on?
    
endif

$(call import-module, android/cpufeatures)

###########
# gles 2.0
###########
include $(CLEAR_VARS)
LOCAL_MODULE            := $(MY_LOCAL_MODULE)-gles20
LOCAL_SHARED_LIBRARIES  := $(MY_LOCAL_SHARED_LIBRARIES)
LOCAL_STATIC_LIBRARIES  := $(MY_LOCAL_STATIC_LIBRARIES)
LOCAL_ARM_MODE          := $(MY_LOCAL_ARM_MODE)
LOCAL_C_INCLUDES        := $(MY_LOCAL_C_INCLUDES)
LOCAL_SRC_FILES         := $(MY_LOCAL_SRC_FILES) $(SRCDIR)/GLES2/UniformSet.cpp $(SRCDIR)/GLES2/GLSLCombiner_gles2.cpp
LOCAL_CFLAGS            := $(MY_LOCAL_CFLAGS) -DGLES2
LOCAL_CPPFLAGS          := $(MY_LOCAL_CPPFLAGS)
LOCAL_LDFLAGS           := $(MY_LOCAL_LDFLAGS)
LOCAL_LDLIBS            := $(MY_LOCAL_LDLIBS) -lGLESv2
include $(BUILD_SHARED_LIBRARY)

###########
# gles 3.0
###########
include $(CLEAR_VARS)
LOCAL_MODULE            := $(MY_LOCAL_MODULE)-gles30
LOCAL_SHARED_LIBRARIES  := $(MY_LOCAL_SHARED_LIBRARIES)
LOCAL_STATIC_LIBRARIES  := $(MY_LOCAL_STATIC_LIBRARIES)
LOCAL_ARM_MODE          := $(MY_LOCAL_ARM_MODE)
LOCAL_C_INCLUDES        := $(MY_LOCAL_C_INCLUDES)
LOCAL_SRC_FILES         := $(MY_LOCAL_SRC_FILES) $(SRCDIR)/OGL3X/UniformBlock.cpp $(SRCDIR)/OGL3X/GLSLCombiner_ogl3x.cpp
LOCAL_CFLAGS            := $(MY_LOCAL_CFLAGS) -DGLES3
LOCAL_CPPFLAGS          := $(MY_LOCAL_CPPFLAGS)
LOCAL_LDFLAGS           := $(MY_LOCAL_LDFLAGS)
LOCAL_LDLIBS            := $(MY_LOCAL_LDLIBS) -lGLESv3
include $(BUILD_SHARED_LIBRARY)


#########################
# mupen64plus-video-rice
#########################
include $(CLEAR_VARS)
SRCDIR := ./mupen64plus-video-rice/src

LOCAL_MODULE := mupen64plus-video-rice
LOCAL_SHARED_LIBRARIES := SDL2
LOCAL_STATIC_LIBRARIES := png
LOCAL_ARM_MODE := arm

LOCAL_C_INCLUDES :=                     \
    $(LOCAL_PATH)/$(SRCDIR)             \
    $(M64P_API_INCLUDES)                \
    $(PNG_INCLUDES)                     \
    $(SDL_INCLUDES)                     \

LOCAL_SRC_FILES :=                      \
    $(SRCDIR)/Blender.cpp               \
    $(SRCDIR)/Combiner.cpp              \
    $(SRCDIR)/CombinerTable.cpp         \
    $(SRCDIR)/Config.cpp                \
    $(SRCDIR)/ConvertImage.cpp          \
    $(SRCDIR)/ConvertImage16.cpp        \
    $(SRCDIR)/Debugger.cpp              \
    $(SRCDIR)/DecodedMux.cpp            \
    $(SRCDIR)/DeviceBuilder.cpp         \
    $(SRCDIR)/FrameBuffer.cpp           \
    $(SRCDIR)/GeneralCombiner.cpp       \
    $(SRCDIR)/GraphicsContext.cpp       \
    $(SRCDIR)/OGLCombiner.cpp           \
    $(SRCDIR)/OGLDecodedMux.cpp         \
    $(SRCDIR)/OGLExtCombiner.cpp        \
    $(SRCDIR)/OGLExtRender.cpp          \
    $(SRCDIR)/OGLES2FragmentShaders.cpp \
    $(SRCDIR)/OGLGraphicsContext.cpp    \
    $(SRCDIR)/OGLRender.cpp             \
    $(SRCDIR)/OGLRenderExt.cpp          \
    $(SRCDIR)/OGLTexture.cpp            \
    $(SRCDIR)/Render.cpp                \
    $(SRCDIR)/RenderBase.cpp            \
    $(SRCDIR)/RenderExt.cpp             \
    $(SRCDIR)/RenderTexture.cpp         \
    $(SRCDIR)/RSP_Parser.cpp            \
    $(SRCDIR)/RSP_S2DEX.cpp             \
    $(SRCDIR)/Texture.cpp               \
    $(SRCDIR)/TextureFilters.cpp        \
    $(SRCDIR)/TextureFilters_2xsai.cpp  \
    $(SRCDIR)/TextureFilters_hq2x.cpp   \
    $(SRCDIR)/TextureFilters_hq4x.cpp   \
    $(SRCDIR)/TextureManager.cpp        \
    $(SRCDIR)/VectorMath.cpp            \
    $(SRCDIR)/Video.cpp                 \
    $(SRCDIR)/osal_dynamiclib_unix.c    \
    $(SRCDIR)/osal_files_unix.c         \
    $(SRCDIR)/liblinux/BMGImage.c       \
    $(SRCDIR)/liblinux/BMGUtils.c       \
    $(SRCDIR)/liblinux/bmp.c            \
    $(SRCDIR)/liblinux/pngrw.c          \

LOCAL_CFLAGS :=         \
    $(COMMON_CFLAGS)    \
    -DANDROID           \
    -DNO_ASM            \
    -DUSE_GLES=1        \
    -fsigned-char       \
    #-DBGR_SHADER        \
    #-DSDL_NO_COMPAT     \
    
LOCAL_CPPFLAGS := $(COMMON_CPPFLAGS)
    
LOCAL_CPP_FEATURES := exceptions

LOCAL_LDFLAGS := -Wl,-version-script,$(LOCAL_PATH)/$(SRCDIR)/video_api_export.ver

LOCAL_LDLIBS :=         \
    -lGLESv2            \
    -llog               \

ifeq ($(TARGET_ARCH_ABI), armeabi-v7a)
    # Use for ARM7a:
    LOCAL_CFLAGS += -mfpu=vfp
    LOCAL_CFLAGS += -mfloat-abi=softfp
    
else ifeq ($(TARGET_ARCH_ABI), armeabi)
    # Use for pre-ARM7a:
    
else ifeq ($(TARGET_ARCH_ABI), x86)
    # TODO: set the proper flags here
    
else
    # Any other architectures that Android could be running on?
    
endif

include $(BUILD_SHARED_LIBRARY)
