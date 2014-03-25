package com.example.office.ui.fragments;

import android.app.Fragment;

import com.example.office.ui.BaseActivity;
import com.msopentech.odatajclient.engine.communication.ODataClientErrorException;

/**
 * Fragment that can handle authorization error.
 */
public abstract class AuthFragment extends Fragment {
    
    /**
     * Indicates if token refresh process is currently running.
     */
    protected boolean mHasToken = true;
    
    /**
     * Handles errors occurred in current fragment. Base implementation handles only HTTP 401 Unauthorized errors.   
     * 
     * @param error an error.
     * @return <tt>true</tt> if error has been handled, <tt>false</tt> otherwise.
     */
    public boolean onError(Throwable error) {
        // handle access token expiration
        if (error instanceof ODataClientErrorException) {
            ODataClientErrorException clientError = (ODataClientErrorException) error;
            if (clientError.getStatusLine().getStatusCode() == 401) {
                ((BaseActivity) getActivity()).getAuthenticator().acquireToken(getActivity());
                mHasToken = false;
                return true;
            }
        }
        
        return false;
    }
}
