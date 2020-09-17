package com.sds.BizAppLauncher.gov.aidl;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public abstract interface IGovServiceCallback extends IInterface{
	public abstract void callback(boolean paramBoolean)	throws RemoteException;

	public static abstract class Stub extends Binder implements IGovServiceCallback	{

		public Stub(){
			attachInterface(this, "com.sds.BizAppLauncher.gov.aidl.IGovServiceCallback");
		}

		public static IGovServiceCallback asInterface(IBinder obj){
			if (obj == null) {
				return null;
			}
			IInterface iin = obj.queryLocalInterface("com.sds.BizAppLauncher.gov.aidl.IGovServiceCallback");
			if ((iin != null) && ((iin instanceof IGovServiceCallback))) {
				return (IGovServiceCallback)iin;
			}
			return new Proxy(obj);
		}

		public IBinder asBinder() {
			return this;
		}

		public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
			switch (code)
			{
			case 1598968902:
				reply.writeString("com.sds.BizAppLauncher.gov.aidl.IGovServiceCallback");
				return true;
			case 1:
				data.enforceInterface("com.sds.BizAppLauncher.gov.aidl.IGovServiceCallback");

				boolean obj = data.readInt() != 0;
				callback(obj);
				reply.writeNoException();
				return true;
			default : break;
			}

			return super.onTransact(code, data, reply, flags);
		}
		private static class Proxy implements IGovServiceCallback {
			IBinder mRemote;

			Proxy(IBinder remote) {
				this.mRemote = remote;
			}

			public IBinder asBinder() {
				return this.mRemote;
			}

			public String getInterfaceDescriptor() {
				return "com.sds.BizAppLauncher.gov.aidl.IGovServiceCallback";
			}

			public void callback(boolean result) throws RemoteException {
				Parcel data = Parcel.obtain();
				Parcel reply = Parcel.obtain();
				try {
					data.writeInterfaceToken("com.sds.BizAppLauncher.gov.aidl.IGovServiceCallback");
					data.writeInt(result ? 1 : 0);
					this.mRemote.transact(1, data, reply, 0);
					reply.readException();
				}
				finally {
					reply.recycle();
					data.recycle();
				}
			}
		}
	}
}