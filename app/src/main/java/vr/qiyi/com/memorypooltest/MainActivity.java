package vr.qiyi.com.memorypooltest;

import android.os.Bundle;
import android.os.IBinder;
import android.os.MemoryFile;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

import com.qiyi.vr.qymemorypool.QYMemoryPoolService;
import com.qiyi.vr.qymemorypool.IQYMemoryPool;

import static android.system.OsConstants.PROT_READ;
import static android.system.OsConstants.PROT_WRITE;


public class MainActivity extends AppCompatActivity {
    private final String TAG = "MemoryPoolTest";
    private IQYMemoryPool mMemoryService = null;
    MemoryFile mFile = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        getMemoryService();
        testShareMemory();
    }

    private void testShareMemory() {
        try {
            String name= new String("testQIYIA");
            int size = mMemoryService.attachShareMemory(name,100);
            MemoryFile memFile = MainActivity.openMemoryFile(mMemoryService.getMemoryHandler(name), size, PROT_WRITE|PROT_READ);

            OutputStream out = memFile.getOutputStream();
            out.write("Hello".getBytes());
            InputStream in = memFile.getInputStream();
            byte[] back = new byte[10];
            in.read(back,0,10);
            Log.d(TAG, "ReadBack:" + back[0] + " ," + back[1] + " ," + back[2]  + " ," + back[3] + " ," + back[4] + " ," + back[5] + " ," + back[6]);
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


       /*String name= new String("testQIYIA");
        byte[] back = new byte[8];


        try {
            int size = mMemoryService.attachShareMemory(name, 100);
            ParcelFileDescriptor file = null;
            try {
                file = ParcelFileDescriptor.dup(mMemoryService.getMemoryHandler(name).getFileDescriptor());
            } catch (IOException e) {
                e.printStackTrace();
            }



            try {
                Field field = FileDescriptor.class.getDeclaredField("descriptor");
                field.setAccessible(true);
                Object fd = field.get(file);
                pfd = ParcelFileDescriptor.fromFd((Integer)fd);
                Log.d(TAG, "file descriptor is " + fd);
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }


           if (file.getStatSize()<0) {
                throw new IllegalStateException("SharedMemory is closed");
            }

            if(file == null) {
                Log.d(TAG, "cant find file descriptor");
            } else {
                Log.d(TAG, "read back file descriptor" + file);
            }
            byte[] value = new byte[]{44,55,66,77};
            long address = 0;
            try {
                address = Os.mmap(0, 100, OsConstants.PROT_READ | OsConstants.PROT_WRITE, OsConstants.MAP_SHARED, file.getFileDescriptor(), 0);
            } catch (ErrnoException e) {
                e.printStackTrace();
            }
            Log.d(TAG, "mmap return address is " + address);



            FileInputStream input = new FileInputStream(file.getFileDescriptor());
            FileOutputStream output = new ParcelFileDescriptor.AutoCloseOutputStream(file);
            back = new byte[8];
            //output.write(value, 0, 2);
            try {

                FileChannel outputChannel = output.getChannel();
                Log.d(TAG, "Output Channel " + outputChannel + ", size " + outputChannel.size() + ", position " + outputChannel.position() );

                input.getChannel().position(0);
                Log.d(TAG, "Input Channel " + input.getChannel() + ", size " + input.getChannel().size() + ", position " + input.getChannel().position() );

                //output.getChannel().position(0);
                //output.write("Hello World!".getBytes());


                input.read(back,0,4);
                outputChannel.position(0);
                outputChannel.write(ByteBuffer.wrap("Hi.".getBytes()));
                //output.write("Hello World!".getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                input.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            mMemoryService.detachShareMemory(name);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        Log.d(TAG, "ReadBack:" + back[0] + " ," + back[1] + " ," + back[2]  + " ," + back[3] + " ," + back[4] + " ," + back[5] + " ," + back[6]);*/

    }

    @Override
    protected void onResume() {
        super.onResume();
        getMemoryService();
        testShareMemory();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    private IQYMemoryPool getMemoryService() {
        if (mMemoryService != null ) return mMemoryService;
        Log.d(TAG, "call getMemoryService");

        try {
            Class serviceManager = Class.forName("android.os.ServiceManager");
            Method getService = serviceManager.getMethod("getService", new Class[] {String.class});
            Log.d(TAG, "calling getService method");
            if(getService != null) {
                Object result = getService.invoke(serviceManager, new Object[]{"QYShareMemory"});
                if(result != null) {
                    IBinder binder = (IBinder) result;
                    //Class cls = Class.forName("com.qiyi.vr.qymemorypool.QYMemoryPoolService");
                    //Method asInterface = cls.getMethod("asInterface", IBinder.class);
                    //Object obj = asInterface.invoke(null, binder);
                    mMemoryService = QYMemoryPoolService.asInterface(binder);

                    Log.d(TAG, "got QYSharedMemory Service !");
                    return mMemoryService;
                }else{
                    Log.d(TAG, "Can't find QYSharedMemory service");
                }
            }else{
                Log.d(TAG, "Can't find getService method");
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }


        return null;
    }
    public static MemoryFile openMemoryFile(ParcelFileDescriptor pfd,int length,int mode){
        if(pfd == null){
            throw new IllegalArgumentException("ParcelFileDescriptor 不能为空");
        }
        FileDescriptor fd = pfd.getFileDescriptor();
        return openMemoryFile(fd,length,mode);
    }

    /**
     * 打开共享内存，一般是一个地方创建了一块共享内存
     * 另一个地方持有描述这块共享内存的文件描述符，调用
     * 此方法即可获得一个描述那块共享内存的MemoryFile
     * 对象
     * @param fd 文件描述
     * @param length 共享内存的大小
     * @param mode PROT_READ = 0x1只读方式打开,
     *             PROT_WRITE = 0x2可写方式打开，
     *             PROT_WRITE|PROT_READ可读可写方式打开
     * @return MemoryFile
     */
    public static MemoryFile openMemoryFile(FileDescriptor fd,int length,int mode){
        MemoryFile memoryFile = null;
        try {
            memoryFile = new MemoryFile("tem",1);
            memoryFile.close();
            Class<?> c = MemoryFile.class;
            Method native_mmap = null;
            Method[] ms = c.getDeclaredMethods();
            for(int i = 0;ms != null&&i<ms.length;i++){
                if(ms[i].getName().equals("native_mmap")){
                    native_mmap = ms[i];
                }
            }
            ReflectUtil.setField("android.os.MemoryFile", memoryFile, "mFD", fd);
            ReflectUtil.setField("android.os.MemoryFile",memoryFile,"mLength",length);
            long address = (long) ReflectUtil.invokeMethod( null, native_mmap, fd, length, mode);
            ReflectUtil.setField("android.os.MemoryFile", memoryFile, "mAddress", address);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return memoryFile;
    }
}
