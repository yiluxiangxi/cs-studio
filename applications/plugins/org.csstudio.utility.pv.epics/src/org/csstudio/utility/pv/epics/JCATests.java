package org.csstudio.utility.pv.epics;

import gov.aps.jca.Channel;
import gov.aps.jca.Context;
import gov.aps.jca.JCALibrary;
import gov.aps.jca.Monitor;
import gov.aps.jca.dbr.DBRType;
import gov.aps.jca.dbr.DBR_TIME_Double;
import gov.aps.jca.event.MonitorEvent;
import gov.aps.jca.event.MonitorListener;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/** JCA Tests
 *  <p>
 *  Must set LD_LIBRARY_PATH to locate JCA lib,
 *  since this is running outside of a plugin.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class JCATests
{
    private static final String PV_NAME = "RFQ_Vac:Pump3:Pressure";
    private static final String ADDR_LIST = "160.91.230.38";
    private static final int TESTRUNS = 10000;

    /** Set <code>true</code> to check for cleanup error. */
    private static final boolean CLEANUP = false;
    private static JCALibrary jca = null;
    private static Context jca_context = null;
    
    final private static DateFormat format =
        new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    
    /** Monitor given PV for some time. */
    static private void run(final String pv_name) throws Exception
    {
        // Lib init
        if (jca == null)
            jca = JCALibrary.getInstance();
        if (jca_context == null)
            jca_context= jca.createContext(JCALibrary.JNI_THREAD_SAFE);
                
        // Connect
        final Channel channel = jca_context.createChannel(pv_name);

        // Subscribe
        MonitorListener monitor = new MonitorListener()
        {
            public void monitorChanged(MonitorEvent event)
            {
                final DBR_TIME_Double value = (DBR_TIME_Double) event.getDBR();
                final Channel source = (Channel)event.getSource();
                System.out.format("%s: %s %f\n",
                        source.getName(),
                        value.getTimeStamp().toMMDDYY(),
                        value.getDoubleValue()[0]);
            }
        };
        final Monitor subscription = channel.addMonitor(DBRType.TIME_DOUBLE, 1, 1, monitor);        
        jca_context.flushIO();

        // Run
        Thread.sleep(2 * 1000);

        // Cleanup. More or less
        subscription.clear();
        channel.destroy();

        // With jca-2.3.0, built against EPICS base R3.14.7,
        // it's OK to clean up.
        //
        // But with jca-2.3.0 or jca-2.3.1, built against EPICS base R3.14.8.2,
        // this results in errors in the jca_context.createChannel() call
        // after handling the first channel with 'CLEANUP':
        // "pthread_create error Invalid argument
        //  CAC: exception during virtual circuit creation:
        //  epicsThread class was unable to  create a new thread"
        if (CLEANUP)
        {
            jca_context.destroy();
            jca_context = null;
            jca = null;
        }
    }

    private static void dumpMeminfo()
    {
        final double MB = 1024.0*1024.0;
        final double free = Runtime.getRuntime().freeMemory() / MB;
        final double total = Runtime.getRuntime().totalMemory() / MB;
        final double max = Runtime.getRuntime().maxMemory() / MB;
        
        System.out.format("%s == JVM Memory: Max %.2f MB, Free %.2f MB (%.1f %%), total %.2f MB (%.1f %%)\n",
                format.format(new Date()), max, free, 100.0*free/max, total, 100.0*total/max);
    }

    public static void main(String[] args) throws Exception
    {
        System.setProperty("gov.aps.jca.jni.JNIContext.addr_list", ADDR_LIST);
        System.setProperty("gov.aps.jca.jni.JNIContext.auto_addr_list", "true");
        
        for (int i=0; i<TESTRUNS; ++i)
        {
            JCATests.run(PV_NAME);
            dumpMeminfo();
        }
    }
}
