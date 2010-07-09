#include <string.h>
#include <jni.h>
#include <unistd.h>
#include <stdlib.h>
#include <signal.h>
#include <fcntl.h>

int main(int argc, const char* argv[]);

static int fdFromChild = -1;
static int fdToChild = -1;

/*
 * Class:     org_petero_droidfish_engine_NativePipedProcess
 * Method:    startProcess
 * Signature: ()V
 */
extern "C" JNIEXPORT void JNICALL Java_org_petero_droidfish_engine_NativePipedProcess_startProcess
		(JNIEnv* env, jobject obj)
{
	int fd1[2];		/* parent -> child */
    int fd2[2];		/* child -> parent */
    if (pipe(fd1) < 0)
    	exit(1);
    if (pipe(fd2) < 0)
    	exit(1);
    int childpid = fork();
    if (childpid == -1) {
        exit(1);
    }
    if (childpid == 0) {
    	close(fd1[1]);
    	close(fd2[0]);
    	close(0); dup(fd1[0]); close(fd1[0]);
    	close(1); dup(fd2[1]); close(fd2[1]);
    	close(2); dup(1);
    	static const char* argv[] = {"stockfish", NULL};
   	    main(1, argv);
    	_exit(0);
    } else {
    	close(fd1[0]);
    	close(fd2[1]);
    	fdFromChild = fd2[0];
    	fdToChild = fd1[1];
    }
}

/*
 * Class:     org_petero_droidfish_engine_NativePipedProcess
 * Method:    readFromProcess
 * Signature: (I)Ljava/lang/String;
 */
extern "C" JNIEXPORT jstring JNICALL Java_org_petero_droidfish_engine_NativePipedProcess_readFromProcess
		(JNIEnv* env, jobject obj, jint timeoutMillis)
{
    fd_set readfds, writefds;
	FD_ZERO(&readfds);
	FD_SET(fdFromChild, &readfds);
	struct timeval tv;
	tv.tv_sec = timeoutMillis / 1000;
	tv.tv_usec = (timeoutMillis % 1000) * 1000;
	int ret = select(fdFromChild + 1, &readfds, NULL, NULL, &tv);
	if (ret < 0)
		return 0;

    fcntl(fdFromChild, F_SETFL, O_NONBLOCK);

    const int bufSize = 32768;
	static char buf[bufSize+1];
	int bufLen = 0;
	bool didWait = false;
	while (bufLen < bufSize) {
		int len = read(fdFromChild, &buf[bufLen], bufSize - bufLen);
		if (len > 0) {
			didWait = false;
			bufLen += len;
		} else {
			if (didWait) {
				break;
			} else {
				usleep(10000);
				didWait = true;
			}
		}
	}
	buf[bufLen] = 0;
    return (*env).NewStringUTF(buf);
}

/*
 * Class:     org_petero_droidfish_engine_NativePipedProcess
 * Method:    writeToProcess
 * Signature: (Ljava/lang/String;)V
 */
extern "C" JNIEXPORT void JNICALL Java_org_petero_droidfish_engine_NativePipedProcess_writeToProcess
		(JNIEnv* env, jobject obj, jstring msg)
{
    const char* str = (*env).GetStringUTFChars(msg, NULL);
    if (str) {
    	int len = strlen(str);
    	int written = 0;
    	while (written < len) {
    		int n = write(fdToChild, &str[written], len - written);
    		if (n <= 0)
    			break;
    		written += n;
    	}
    }
}
