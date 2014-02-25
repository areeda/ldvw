#ifndef _JAVAWEBSHELLSERVICES_H_
#define _JAVAWEBSHELLSERVICES_H_

#include "nsIWebShellServices.h"

/**
 * A simple implementation of nsIWebShellServices that allows us to determine
 * whether parsing was interrupted to change character sets.
 */
class JavaWebShellServices : public nsIWebShellServices
{
public:
	JavaWebShellServices();
	~JavaWebShellServices();
	void reset();

	NS_DECL_ISUPPORTS
	NS_DECL_NSIWEBSHELLSERVICES

	bool wasStopped() const              { return _wasStopped; }
	const char *getNewCharset() const    { return _newCharset; }
	PRUint32 getNewCharsetSource() const { return _newCharsetSource; }

private:
	bool _wasStopped;
	char *_newCharset;
	PRUint32 _newCharsetSource;
};

#endif // _JAVAWEBSHELLSERVICES_H_
