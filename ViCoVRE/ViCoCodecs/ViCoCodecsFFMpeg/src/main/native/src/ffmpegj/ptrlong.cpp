#include <jni.h>
#include <string.h>

extern jlong ptr2jlong(void *ptr);
extern void *jlong2ptr(jlong jl);

jlong ptr2jlong(void *ptr) {
    jlong jl = 0;
    if (sizeof(void *) > sizeof(jlong)) {
        fprintf(stderr, "sizeof(void *) > sizeof(jlong)\n");
        return 0;
    }

    memcpy(&jl, &ptr, sizeof(void *));
    return jl;
}

void *jlong2ptr(jlong jl) {

    void *ptr = 0;
    if (sizeof(void *) > sizeof(jlong)) {
        fprintf(stderr, "sizeof(void *) > sizeof(jlong)\n");
        return 0;
    }

    memcpy(&ptr, &jl, sizeof(void *));
    return ptr;
}
