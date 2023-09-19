import arc.graphics.GL20
import java.nio.Buffer
import java.nio.FloatBuffer
import java.nio.IntBuffer

class FakeGraphics : GL20 {
    override fun glActiveTexture(texture: Int) {
        TODO("Not yet implemented")
    }

    override fun glBindTexture(target: Int, texture: Int) {
        TODO("Not yet implemented")
    }

    override fun glBlendFunc(sfactor: Int, dfactor: Int) {
        TODO("Not yet implemented")
    }

    override fun glClear(mask: Int) {
        TODO("Not yet implemented")
    }

    override fun glClearColor(red: Float, green: Float, blue: Float, alpha: Float) {
        TODO("Not yet implemented")
    }

    override fun glClearDepthf(depth: Float) {
        TODO("Not yet implemented")
    }

    override fun glClearStencil(s: Int) {
        TODO("Not yet implemented")
    }

    override fun glColorMask(red: Boolean, green: Boolean, blue: Boolean, alpha: Boolean) {
        TODO("Not yet implemented")
    }

    override fun glCompressedTexImage2D(
        target: Int,
        level: Int,
        internalformat: Int,
        width: Int,
        height: Int,
        border: Int,
        imageSize: Int,
        data: Buffer?
    ) {
        TODO("Not yet implemented")
    }

    override fun glCompressedTexSubImage2D(
        target: Int,
        level: Int,
        xoffset: Int,
        yoffset: Int,
        width: Int,
        height: Int,
        format: Int,
        imageSize: Int,
        data: Buffer?
    ) {
        TODO("Not yet implemented")
    }

    override fun glCopyTexImage2D(
        target: Int,
        level: Int,
        internalformat: Int,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        border: Int
    ) {
        TODO("Not yet implemented")
    }

    override fun glCopyTexSubImage2D(
        target: Int,
        level: Int,
        xoffset: Int,
        yoffset: Int,
        x: Int,
        y: Int,
        width: Int,
        height: Int
    ) {
        TODO("Not yet implemented")
    }

    override fun glCullFace(mode: Int) {
        TODO("Not yet implemented")
    }

    override fun glDeleteTexture(texture: Int) {
        TODO("Not yet implemented")
    }

    override fun glDepthFunc(func: Int) {
        TODO("Not yet implemented")
    }

    override fun glDepthMask(flag: Boolean) {
        TODO("Not yet implemented")
    }

    override fun glDepthRangef(zNear: Float, zFar: Float) {
        TODO("Not yet implemented")
    }

    override fun glDisable(cap: Int) {
        TODO("Not yet implemented")
    }

    override fun glDrawArrays(mode: Int, first: Int, count: Int) {
        TODO("Not yet implemented")
    }

    override fun glDrawElements(mode: Int, count: Int, type: Int, indices: Buffer?) {
        TODO("Not yet implemented")
    }

    override fun glDrawElements(mode: Int, count: Int, type: Int, indices: Int) {
        TODO("Not yet implemented")
    }

    override fun glEnable(cap: Int) {
        TODO("Not yet implemented")
    }

    override fun glFinish() {
        TODO("Not yet implemented")
    }

    override fun glFlush() {
        TODO("Not yet implemented")
    }

    override fun glFrontFace(mode: Int) {
        TODO("Not yet implemented")
    }

    override fun glGenTexture(): Int {
        TODO("Not yet implemented")
    }

    override fun glGetError(): Int {
        TODO("Not yet implemented")
    }

    override fun glGetIntegerv(pname: Int, params: IntBuffer?) {

    }

    override fun glGetString(name: Int): String {
        TODO("Not yet implemented")
    }

    override fun glHint(target: Int, mode: Int) {
        TODO("Not yet implemented")
    }

    override fun glLineWidth(width: Float) {
        TODO("Not yet implemented")
    }

    override fun glPixelStorei(pname: Int, param: Int) {
        TODO("Not yet implemented")
    }

    override fun glPolygonOffset(factor: Float, units: Float) {
        TODO("Not yet implemented")
    }

    override fun glReadPixels(x: Int, y: Int, width: Int, height: Int, format: Int, type: Int, pixels: Buffer?) {
        TODO("Not yet implemented")
    }

    override fun glScissor(x: Int, y: Int, width: Int, height: Int) {
        TODO("Not yet implemented")
    }

    override fun glStencilFunc(func: Int, ref: Int, mask: Int) {
        TODO("Not yet implemented")
    }

    override fun glStencilMask(mask: Int) {
        TODO("Not yet implemented")
    }

    override fun glStencilOp(fail: Int, zfail: Int, zpass: Int) {
        TODO("Not yet implemented")
    }

    override fun glTexImage2D(
        target: Int,
        level: Int,
        internalformat: Int,
        width: Int,
        height: Int,
        border: Int,
        format: Int,
        type: Int,
        pixels: Buffer?
    ) {
        TODO("Not yet implemented")
    }

    override fun glTexParameterf(target: Int, pname: Int, param: Float) {
        TODO("Not yet implemented")
    }

    override fun glTexSubImage2D(
        target: Int,
        level: Int,
        xoffset: Int,
        yoffset: Int,
        width: Int,
        height: Int,
        format: Int,
        type: Int,
        pixels: Buffer?
    ) {
        TODO("Not yet implemented")
    }

    override fun glViewport(x: Int, y: Int, width: Int, height: Int) {

    }

    override fun glAttachShader(program: Int, shader: Int) {
        TODO("Not yet implemented")
    }

    override fun glBindAttribLocation(program: Int, index: Int, name: String?) {
        TODO("Not yet implemented")
    }

    override fun glBindBuffer(target: Int, buffer: Int) {
        TODO("Not yet implemented")
    }

    override fun glBindFramebuffer(target: Int, framebuffer: Int) {
        TODO("Not yet implemented")
    }

    override fun glBindRenderbuffer(target: Int, renderbuffer: Int) {
        TODO("Not yet implemented")
    }

    override fun glBlendColor(red: Float, green: Float, blue: Float, alpha: Float) {
        TODO("Not yet implemented")
    }

    override fun glBlendEquation(mode: Int) {
        TODO("Not yet implemented")
    }

    override fun glBlendEquationSeparate(modeRGB: Int, modeAlpha: Int) {
        TODO("Not yet implemented")
    }

    override fun glBlendFuncSeparate(srcRGB: Int, dstRGB: Int, srcAlpha: Int, dstAlpha: Int) {
        TODO("Not yet implemented")
    }

    override fun glBufferData(target: Int, size: Int, data: Buffer?, usage: Int) {
        TODO("Not yet implemented")
    }

    override fun glBufferSubData(target: Int, offset: Int, size: Int, data: Buffer?) {
        TODO("Not yet implemented")
    }

    override fun glCheckFramebufferStatus(target: Int): Int {
        TODO("Not yet implemented")
    }

    override fun glCompileShader(shader: Int) {
        TODO("Not yet implemented")
    }

    override fun glCreateProgram(): Int {
        TODO("Not yet implemented")
    }

    override fun glCreateShader(type: Int): Int {
        return 0
    }

    override fun glDeleteBuffer(buffer: Int) {
        TODO("Not yet implemented")
    }

    override fun glDeleteFramebuffer(framebuffer: Int) {
        TODO("Not yet implemented")
    }

    override fun glDeleteProgram(program: Int) {
        TODO("Not yet implemented")
    }

    override fun glDeleteRenderbuffer(renderbuffer: Int) {
        TODO("Not yet implemented")
    }

    override fun glDeleteShader(shader: Int) {
        TODO("Not yet implemented")
    }

    override fun glDetachShader(program: Int, shader: Int) {
        TODO("Not yet implemented")
    }

    override fun glDisableVertexAttribArray(index: Int) {
        TODO("Not yet implemented")
    }

    override fun glEnableVertexAttribArray(index: Int) {
        TODO("Not yet implemented")
    }

    override fun glFramebufferRenderbuffer(target: Int, attachment: Int, renderbuffertarget: Int, renderbuffer: Int) {
        TODO("Not yet implemented")
    }

    override fun glFramebufferTexture2D(target: Int, attachment: Int, textarget: Int, texture: Int, level: Int) {
        TODO("Not yet implemented")
    }

    override fun glGenBuffer(): Int {
        TODO("Not yet implemented")
    }

    override fun glGenerateMipmap(target: Int) {
        TODO("Not yet implemented")
    }

    override fun glGenFramebuffer(): Int {
        TODO("Not yet implemented")
    }

    override fun glGenRenderbuffer(): Int {
        TODO("Not yet implemented")
    }

    override fun glGetActiveAttrib(program: Int, index: Int, size: IntBuffer?, type: IntBuffer?): String {
        TODO("Not yet implemented")
    }

    override fun glGetActiveUniform(program: Int, index: Int, size: IntBuffer?, type: IntBuffer?): String {
        TODO("Not yet implemented")
    }

    override fun glGetAttribLocation(program: Int, name: String?): Int {
        TODO("Not yet implemented")
    }

    override fun glGetBooleanv(pname: Int, params: Buffer?) {
        TODO("Not yet implemented")
    }

    override fun glGetBufferParameteriv(target: Int, pname: Int, params: IntBuffer?) {
        TODO("Not yet implemented")
    }

    override fun glGetFloatv(pname: Int, params: FloatBuffer?) {
        TODO("Not yet implemented")
    }

    override fun glGetFramebufferAttachmentParameteriv(target: Int, attachment: Int, pname: Int, params: IntBuffer?) {
        TODO("Not yet implemented")
    }

    override fun glGetProgramiv(program: Int, pname: Int, params: IntBuffer?) {
        TODO("Not yet implemented")
    }

    override fun glGetProgramInfoLog(program: Int): String {
        TODO("Not yet implemented")
    }

    override fun glGetRenderbufferParameteriv(target: Int, pname: Int, params: IntBuffer?) {
        TODO("Not yet implemented")
    }

    override fun glGetShaderiv(shader: Int, pname: Int, params: IntBuffer?) {
        TODO("Not yet implemented")
    }

    override fun glGetShaderInfoLog(shader: Int): String {
        TODO("Not yet implemented")
    }

    override fun glGetShaderPrecisionFormat(
        shadertype: Int,
        precisiontype: Int,
        range: IntBuffer?,
        precision: IntBuffer?
    ) {
        TODO("Not yet implemented")
    }

    override fun glGetTexParameterfv(target: Int, pname: Int, params: FloatBuffer?) {
        TODO("Not yet implemented")
    }

    override fun glGetTexParameteriv(target: Int, pname: Int, params: IntBuffer?) {
        TODO("Not yet implemented")
    }

    override fun glGetUniformfv(program: Int, location: Int, params: FloatBuffer?) {
        TODO("Not yet implemented")
    }

    override fun glGetUniformiv(program: Int, location: Int, params: IntBuffer?) {
        TODO("Not yet implemented")
    }

    override fun glGetUniformLocation(program: Int, name: String?): Int {
        TODO("Not yet implemented")
    }

    override fun glGetVertexAttribfv(index: Int, pname: Int, params: FloatBuffer?) {
        TODO("Not yet implemented")
    }

    override fun glGetVertexAttribiv(index: Int, pname: Int, params: IntBuffer?) {
        TODO("Not yet implemented")
    }

    override fun glIsBuffer(buffer: Int): Boolean {
        TODO("Not yet implemented")
    }

    override fun glIsEnabled(cap: Int): Boolean {
        TODO("Not yet implemented")
    }

    override fun glIsFramebuffer(framebuffer: Int): Boolean {
        TODO("Not yet implemented")
    }

    override fun glIsProgram(program: Int): Boolean {
        TODO("Not yet implemented")
    }

    override fun glIsRenderbuffer(renderbuffer: Int): Boolean {
        TODO("Not yet implemented")
    }

    override fun glIsShader(shader: Int): Boolean {
        TODO("Not yet implemented")
    }

    override fun glIsTexture(texture: Int): Boolean {
        TODO("Not yet implemented")
    }

    override fun glLinkProgram(program: Int) {
        TODO("Not yet implemented")
    }

    override fun glReleaseShaderCompiler() {
        TODO("Not yet implemented")
    }

    override fun glRenderbufferStorage(target: Int, internalformat: Int, width: Int, height: Int) {
        TODO("Not yet implemented")
    }

    override fun glSampleCoverage(value: Float, invert: Boolean) {
        TODO("Not yet implemented")
    }

    override fun glShaderSource(shader: Int, string: String?) {
        TODO("Not yet implemented")
    }

    override fun glStencilFuncSeparate(face: Int, func: Int, ref: Int, mask: Int) {
        TODO("Not yet implemented")
    }

    override fun glStencilMaskSeparate(face: Int, mask: Int) {
        TODO("Not yet implemented")
    }

    override fun glStencilOpSeparate(face: Int, fail: Int, zfail: Int, zpass: Int) {
        TODO("Not yet implemented")
    }

    override fun glTexParameterfv(target: Int, pname: Int, params: FloatBuffer?) {
        TODO("Not yet implemented")
    }

    override fun glTexParameteri(target: Int, pname: Int, param: Int) {
        TODO("Not yet implemented")
    }

    override fun glTexParameteriv(target: Int, pname: Int, params: IntBuffer?) {
        TODO("Not yet implemented")
    }

    override fun glUniform1f(location: Int, x: Float) {
        TODO("Not yet implemented")
    }

    override fun glUniform1fv(location: Int, count: Int, v: FloatBuffer?) {
        TODO("Not yet implemented")
    }

    override fun glUniform1fv(location: Int, count: Int, v: FloatArray?, offset: Int) {
        TODO("Not yet implemented")
    }

    override fun glUniform1i(location: Int, x: Int) {
        TODO("Not yet implemented")
    }

    override fun glUniform1iv(location: Int, count: Int, v: IntBuffer?) {
        TODO("Not yet implemented")
    }

    override fun glUniform1iv(location: Int, count: Int, v: IntArray?, offset: Int) {
        TODO("Not yet implemented")
    }

    override fun glUniform2f(location: Int, x: Float, y: Float) {
        TODO("Not yet implemented")
    }

    override fun glUniform2fv(location: Int, count: Int, v: FloatBuffer?) {
        TODO("Not yet implemented")
    }

    override fun glUniform2fv(location: Int, count: Int, v: FloatArray?, offset: Int) {
        TODO("Not yet implemented")
    }

    override fun glUniform2i(location: Int, x: Int, y: Int) {
        TODO("Not yet implemented")
    }

    override fun glUniform2iv(location: Int, count: Int, v: IntBuffer?) {
        TODO("Not yet implemented")
    }

    override fun glUniform2iv(location: Int, count: Int, v: IntArray?, offset: Int) {
        TODO("Not yet implemented")
    }

    override fun glUniform3f(location: Int, x: Float, y: Float, z: Float) {
        TODO("Not yet implemented")
    }

    override fun glUniform3fv(location: Int, count: Int, v: FloatBuffer?) {
        TODO("Not yet implemented")
    }

    override fun glUniform3fv(location: Int, count: Int, v: FloatArray?, offset: Int) {
        TODO("Not yet implemented")
    }

    override fun glUniform3i(location: Int, x: Int, y: Int, z: Int) {
        TODO("Not yet implemented")
    }

    override fun glUniform3iv(location: Int, count: Int, v: IntBuffer?) {
        TODO("Not yet implemented")
    }

    override fun glUniform3iv(location: Int, count: Int, v: IntArray?, offset: Int) {
        TODO("Not yet implemented")
    }

    override fun glUniform4f(location: Int, x: Float, y: Float, z: Float, w: Float) {
        TODO("Not yet implemented")
    }

    override fun glUniform4fv(location: Int, count: Int, v: FloatBuffer?) {
        TODO("Not yet implemented")
    }

    override fun glUniform4fv(location: Int, count: Int, v: FloatArray?, offset: Int) {
        TODO("Not yet implemented")
    }

    override fun glUniform4i(location: Int, x: Int, y: Int, z: Int, w: Int) {
        TODO("Not yet implemented")
    }

    override fun glUniform4iv(location: Int, count: Int, v: IntBuffer?) {
        TODO("Not yet implemented")
    }

    override fun glUniform4iv(location: Int, count: Int, v: IntArray?, offset: Int) {
        TODO("Not yet implemented")
    }

    override fun glUniformMatrix2fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer?) {
        TODO("Not yet implemented")
    }

    override fun glUniformMatrix2fv(location: Int, count: Int, transpose: Boolean, value: FloatArray?, offset: Int) {
        TODO("Not yet implemented")
    }

    override fun glUniformMatrix3fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer?) {
        TODO("Not yet implemented")
    }

    override fun glUniformMatrix3fv(location: Int, count: Int, transpose: Boolean, value: FloatArray?, offset: Int) {
        TODO("Not yet implemented")
    }

    override fun glUniformMatrix4fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer?) {
        TODO("Not yet implemented")
    }

    override fun glUniformMatrix4fv(location: Int, count: Int, transpose: Boolean, value: FloatArray?, offset: Int) {
        TODO("Not yet implemented")
    }

    override fun glUseProgram(program: Int) {
        TODO("Not yet implemented")
    }

    override fun glValidateProgram(program: Int) {
        TODO("Not yet implemented")
    }

    override fun glVertexAttrib1f(indx: Int, x: Float) {
        TODO("Not yet implemented")
    }

    override fun glVertexAttrib1fv(indx: Int, values: FloatBuffer?) {
        TODO("Not yet implemented")
    }

    override fun glVertexAttrib2f(indx: Int, x: Float, y: Float) {
        TODO("Not yet implemented")
    }

    override fun glVertexAttrib2fv(indx: Int, values: FloatBuffer?) {
        TODO("Not yet implemented")
    }

    override fun glVertexAttrib3f(indx: Int, x: Float, y: Float, z: Float) {
        TODO("Not yet implemented")
    }

    override fun glVertexAttrib3fv(indx: Int, values: FloatBuffer?) {
        TODO("Not yet implemented")
    }

    override fun glVertexAttrib4f(indx: Int, x: Float, y: Float, z: Float, w: Float) {
        TODO("Not yet implemented")
    }

    override fun glVertexAttrib4fv(indx: Int, values: FloatBuffer?) {
        TODO("Not yet implemented")
    }

    override fun glVertexAttribPointer(
        indx: Int,
        size: Int,
        type: Int,
        normalized: Boolean,
        stride: Int,
        ptr: Buffer?
    ) {
        TODO("Not yet implemented")
    }

    override fun glVertexAttribPointer(indx: Int, size: Int, type: Int, normalized: Boolean, stride: Int, ptr: Int) {
        TODO("Not yet implemented")
    }

}