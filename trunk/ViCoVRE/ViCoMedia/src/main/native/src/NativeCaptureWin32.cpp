#include "NativeCaptureWin32.h"

static jlong ptr2jlong(void *ptr) {
    jlong jl = 0;
    if (sizeof(void *) > sizeof(jlong)) {
        fprintf(stderr, "sizeof(void *) > sizeof(jlong)\n");
        return 0;
    }

    memcpy(&jl, &ptr, sizeof(void *));
    return jl;
}

static void *jlong2ptr(jlong jl) {

    void *ptr = 0;
    if (sizeof(void *) > sizeof(jlong)) {
        fprintf(stderr, "sizeof(void *) > sizeof(jlong)\n");
        return 0;
    }

    memcpy(&ptr, &jl, sizeof(void *));
    return ptr;
}

NativeCapture *getNativeCapture(JNIEnv *env, jobject obj) {
    jclass cls = env->GetObjectClass(obj);
    jmethodID getPeerMethod = env->GetMethodID(cls, "getPeer", "()J");
    return (NativeCapture *) jlong2ptr(env->CallLongMethod(obj, getPeerMethod));
}

JNIEXPORT void JNICALL
    Java_com_googlecode_vicovre_media_protocol_screen_NativeCapture_init
        (JNIEnv *env, jobject obj, jint x, jint y, jint width, jint height) {
    NativeCapture *nc = new NativeCapture(x, y, width, height);
    jclass cls = env->GetObjectClass(obj);
    jmethodID setPeerMethod = env->GetMethodID(cls, "setPeer", "(J)V");
    env->CallVoidMethod(obj, setPeerMethod, ptr2jlong(nc));
}

JNIEXPORT void JNICALL
    Java_com_googlecode_vicovre_media_protocol_screen_NativeCapture_captureScreen
        (JNIEnv *env, jobject obj, jbyteArray buffer) {
    NativeCapture *nc = getNativeCapture(env, obj);
    char *data = (char *) env->GetPrimitiveArrayCritical(buffer, 0);
    nc->capture(data);
    env->ReleasePrimitiveArrayCritical(buffer, data, 0);
}

JNIEXPORT void JNICALL
    Java_com_googlecode_vicovre_media_protocol_screen_NativeCapture_close
        (JNIEnv *env, jobject obj) {
    NativeCapture *nc = getNativeCapture(env, obj);
    delete nc;
}

NativeCapture::NativeCapture(int x, int y, int width, int height) {
    hScreenDC = GetWindowDC(0);
    hmemDC = CreateCompatibleDC(hScreenDC);
    this->width = width;
    this->height = height;
    this->x = x;
    this->y = y;
    hmemBM = CreateCompatibleBitmap(hScreenDC, width, height);
    SelectObject(hmemDC, hmemBM);
    bmInfo.bmiHeader.biSize = sizeof(bmInfo.bmiHeader);
    bmInfo.bmiHeader.biWidth = width;
    bmInfo.bmiHeader.biHeight = -height;
    bmInfo.bmiHeader.biPlanes = 1;
    bmInfo.bmiHeader.biBitCount = 24;
    bmInfo.bmiHeader.biCompression = BI_RGB;
    bmInfo.bmiHeader.biSizeImage = 0;
    bmInfo.bmiHeader.biXPelsPerMeter = 0;
    bmInfo.bmiHeader.biYPelsPerMeter = 0;
    bmInfo.bmiHeader.biClrUsed = 0;
    bmInfo.bmiHeader.biClrImportant = 0;
}


NativeCapture::~NativeCapture() {
    DeleteObject(hmemBM);
    DeleteDC(hmemDC);
    ReleaseDC(0, hScreenDC);
}

void NativeCapture::capture(char *buffer) {
    BitBlt(hmemDC, 0, 0, width, height, hScreenDC, x, y, SRCCOPY);
    CURSORINFO curInf;
    curInf.cbSize = sizeof(curInf);
    GetCursorInfo(&curInf);

    if (curInf.flags == CURSOR_SHOWING) {
        GetCursorPos(&curPos);
        GetIconInfo(curInf.hCursor, &iconInfo);
        int x = curPos.x - this->x - iconInfo.xHotspot;
        int y = curPos.y - this->y - iconInfo.yHotspot;
        if ((x > 0) && (x < width) && (y > 0) && (y < height)) {
            DrawIcon(hmemDC, x, y, curInf.hCursor);
        }
    }
    GetDIBits(hmemDC, hmemBM, 0, height, buffer, &bmInfo, DIB_RGB_COLORS);
}
