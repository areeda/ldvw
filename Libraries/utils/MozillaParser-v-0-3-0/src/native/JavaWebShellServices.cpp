#include "JavaWebShellServices.h"
#include "nscore.h"
#include <stdlib.h>

JavaWebShellServices::JavaWebShellServices()
	: _wasStopped(false)
	, _newCharset(NULL)
	, _newCharsetSource(kCharsetUninitialized)
{
}

JavaWebShellServices::~JavaWebShellServices()
{
	this->reset();
}

void
JavaWebShellServices::reset()
{
	_wasStopped = false;
	_newCharsetSource = kCharsetUninitialized;

	if(_newCharset)
	{
		free( _newCharset );
		_newCharset = NULL;
	}
}

NS_IMPL_ISUPPORTS1(JavaWebShellServices, nsIWebShellServices)

NS_IMETHODIMP
JavaWebShellServices::ReloadDocument(const char *aCharset, PRInt32 aSource)
{
	if( aCharset == nsnull )
	{
		if( _newCharset )
		{
			free(_newCharset);
			_newCharset = NULL;
		}
	}
	else
	{
		_newCharset = strdup(aCharset);
	}

	_newCharsetSource = aSource;

	return NS_OK;
}

NS_IMETHODIMP
JavaWebShellServices::StopDocumentLoad(void)
{
	_wasStopped = true;
	return NS_OK;
}

NS_IMETHODIMP
JavaWebShellServices::SetRendering(PRBool aRender)
{
	return NS_OK;
}
