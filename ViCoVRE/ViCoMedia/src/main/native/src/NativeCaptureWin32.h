#include "com_googlecode_vicovre_media_protocol_screen_NativeCapture.h"
#include <windows.h>

class NativeCapture {
    private:
        HDC hScreenDC;
        HDC hmemDC;
        int x;
        int y;
        int width;
        int height;
        HBITMAP hmemBM;
        BITMAPINFO bmInfo;
        POINT curPos;
        ICONINFO iconInfo;

    public:
        NativeCapture(int x, int y, int width, int height);
        ~NativeCapture();
        void capture(char *buffer);
};
