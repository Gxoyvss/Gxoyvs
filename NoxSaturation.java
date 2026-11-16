package com.example.noxvisuals;

import net.minecraft.client.MinecraftClient;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.nio.ByteBuffer;

/**
 * Насыщенность цвета на весь экран (как Color Saturation в Lunar Client).
 * После отрисовки мира копируем кадр в свою текстуру и рисуем его обратно через маленький шейдер:
 * цвет = mix(серый, цвет, насыщенность). 0 = чёрно-белый, 1 = как обычно, 2 = очень яркий.
 * Работает на чистом OpenGL (LWJGL), состояние GL сохраняется и возвращается как было.
 */
public class NoxSaturation {
    static int prog, vao, tex, texW, texH, locTex, locSat;
    static boolean failed;

    static final String VERT = "#version 150\n"
        + "out vec2 uv;\n"
        + "void main() {\n"
        + "    vec2 p = vec2((gl_VertexID << 1) & 2, gl_VertexID & 2);\n"
        + "    uv = p;\n"
        + "    gl_Position = vec4(p * 2.0 - 1.0, 0.0, 1.0);\n"
        + "}\n";

    static final String FRAG = "#version 150\n"
        + "uniform sampler2D Tex;\n"
        + "uniform float Sat;\n"
        + "in vec2 uv;\n"
        + "out vec4 outColor;\n"
        + "void main() {\n"
        + "    vec4 c = texture(Tex, uv);\n"
        + "    float l = dot(c.rgb, vec3(0.299, 0.587, 0.114));\n"
        + "    outColor = vec4(clamp(mix(vec3(l), c.rgb, Sat), 0.0, 1.0), c.a);\n"
        + "}\n";

    static boolean active() {
        return NoxConfig.colorSat && Math.abs(NoxConfig.satValue - 1f) > 0.01f;
    }

    /** Вызывается из миксина сразу после отрисовки мира (до интерфейса). */
    public static void apply(MinecraftClient mc) {
        if (failed || !active()) return;
        try {
            draw(mc);
        } catch (Throwable t) {
            failed = true;   // при любой ошибке выключаем эффект, чтобы не ломать игру
            System.err.println("[NoxVisuals] Saturation disabled: " + t);
        }
    }

    static void draw(MinecraftClient mc) {
        int w = mc.getWindow().getFramebufferWidth(), h = mc.getWindow().getFramebufferHeight();
        if (w <= 0 || h <= 0) return;

        // запоминаем состояние GL, которое трогаем
        int prevActive = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        int prevProg = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        int prevVao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        boolean blend = GL11.glIsEnabled(GL11.GL_BLEND);
        boolean depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        boolean scissor = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        int prevTex = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);

        if (prog == 0) init();

        // копия текущего кадра (читаем из привязанного главного буфера кадра)
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, tex);
        if (w != texW || h != texH) {
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, w, h, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (ByteBuffer) null);
            texW = w;
            texH = h;
        }
        GL11.glCopyTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, 0, 0, w, h);

        // рисуем кадр обратно через шейдер насыщенности
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        GL20.glUseProgram(prog);
        GL20.glUniform1i(locTex, 0);
        GL20.glUniform1f(locSat, NoxConfig.satValue);
        GL30.glBindVertexArray(vao);
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 3);

        // возвращаем всё как было
        GL30.glBindVertexArray(prevVao);
        GL20.glUseProgram(prevProg);
        if (blend) GL11.glEnable(GL11.GL_BLEND);
        if (depth) GL11.glEnable(GL11.GL_DEPTH_TEST);
        if (cull) GL11.glEnable(GL11.GL_CULL_FACE);
        if (scissor) GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, prevTex);
        GL13.glActiveTexture(prevActive);
    }

    static void init() {
        int v = compile(GL20.GL_VERTEX_SHADER, VERT);
        int f = compile(GL20.GL_FRAGMENT_SHADER, FRAG);
        int p = GL20.glCreateProgram();
        GL20.glAttachShader(p, v);
        GL20.glAttachShader(p, f);
        GL20.glLinkProgram(p);
        if (GL20.glGetProgrami(p, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
            throw new IllegalStateException("link: " + GL20.glGetProgramInfoLog(p));
        }
        GL20.glDeleteShader(v);
        GL20.glDeleteShader(f);
        locTex = GL20.glGetUniformLocation(p, "Tex");
        locSat = GL20.glGetUniformLocation(p, "Sat");
        vao = GL30.glGenVertexArrays();
        tex = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, tex);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12_CLAMP);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12_CLAMP);
        prog = p;
    }

    static final int GL12_CLAMP = 0x812F;   // GL_CLAMP_TO_EDGE

    static int compile(int type, String src) {
        int s = GL20.glCreateShader(type);
        GL20.glShaderSource(s, src);
        GL20.glCompileShader(s);
        if (GL20.glGetShaderi(s, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            throw new IllegalStateException("shader: " + GL20.glGetShaderInfoLog(s));
        }
        return s;
    }
}
