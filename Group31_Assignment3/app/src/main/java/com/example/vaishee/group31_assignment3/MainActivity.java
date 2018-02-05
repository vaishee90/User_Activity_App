package com.example.vaishee.group31_assignment3;

import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.channels.FileChannel;
import java.util.StringTokenizer;
import java.util.Vector;

import libsvm.svm;
import libsvm.svm_model;
import libsvm.svm_node;
import libsvm.svm_parameter;
import libsvm.svm_print_interface;
import libsvm.svm_problem;

public class MainActivity extends AppCompatActivity {

    static Intent startSensorService;
    static SQLiteDatabase db;
    static String table_name = "reading_Table";
    private svm_parameter param;		// set by parse_command_line
    private svm_problem prob;		// set by read_problem
    private svm_model model;
    private String input_file_name;		// set by parse_command_line
    private String model_file_name;		// set by parse_command_line
    private String error_msg;
    private int cross_validation;
    private int nr_fold;
    //public String currentDBPath;
    public String packagename;
    public String accuracy, svm_params;

    TextView classifier, accuracy_text, svm_parameters;

    FileReader fin;
    BufferedReader br;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            File sd = Environment.getExternalStorageDirectory();
            File data = Environment.getDataDirectory();

            if (sd.canWrite()) {
                String currentDBPath = "/data/data/" + getPackageName() + "/databases/RecordsDB.db";
                String backupDBPath = "/sdcard/RecordsDB.db";
                File currentDB = new File(currentDBPath);
                File backupDB = new File(sd, backupDBPath);

                if (currentDB.exists()) {
                    FileChannel src = new FileInputStream(currentDB).getChannel();
                    FileChannel dst = new FileOutputStream(backupDB).getChannel();
                    dst.transferFrom(src, 0, src.size());
                    src.close();
                    dst.close();
                }
            }
        } catch (Exception e) {
             e.printStackTrace();
        }

        Button eating = (Button) findViewById(R.id.eating);
        Button walking = (Button) findViewById(R.id.walking);
        Button running = (Button) findViewById(R.id.running);

        //user starts testing
        Button start_test = (Button) findViewById(R.id.start_test);

        //predict activity
        Button predict = (Button) findViewById(R.id.predict);

        classifier = (TextView) findViewById(R.id.activity_name);
        accuracy_text = (TextView) findViewById(R.id.show_accuracy);
        svm_parameters = (TextView) findViewById(R.id.svm_params);


        packagename = "com.example.vaishee.group31_assignment3";

        db = openOrCreateDatabase("RecordsDB.db", MODE_PRIVATE, null);
        StringBuilder columnNames = new StringBuilder();
        for (int i = 1; i <= 50; i++)
            columnNames.append(",Accel_x" + i + " float,Accel_y" + i + " float,Accel_z" + i + " float");
        String schema = columnNames.toString();
        Log.d("Schema", schema);
        String createSql = "id int" + schema +  ",label text";
        //db.delete(table_name, null, null);
        db.execSQL("create table if not exists " + table_name + " (" + createSql +");");
        System.out.println("Creating table...");

        start_test.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                db.execSQL("delete from " + table_name + " where id = 4");
                startSensorService = new Intent(MainActivity.this, TestData.class);
                Bundle b = new Bundle();
                b.putInt("index", 4);
                b.putString("table_name", table_name);
                b.putString("label", "");
                startSensorService.putExtras(b);
                System.out.println("Starting service..");
                startService(startSensorService);
            }
        });

        eating.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                startSensorService = new Intent(MainActivity.this, SensorInit.class);
                Bundle b = new Bundle();
                b.putInt("index",1);
                b.putString("table_name", table_name);
                b.putString("label","eating");
                startSensorService.putExtras(b);
                System.out.println("Starting service..");
                startService(startSensorService);
            }
        });

        walking.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                startSensorService = new Intent(MainActivity.this, SensorInit.class);
                Bundle b = new Bundle();
                b.putInt("index",2);
                b.putString("table_name", table_name);
                b.putString("label","walking");
                startSensorService.putExtras(b);
                System.out.println("Starting service..");
                startService(startSensorService);
            }
        });

        running.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                startSensorService = new Intent(MainActivity.this, SensorInit.class);
                Bundle b = new Bundle();
                b.putInt("index",3);
                b.putString("table_name", table_name);
                b.putString("label","running");
                startSensorService.putExtras(b);
                System.out.println("Starting service..");
                startService(startSensorService);
            }
        });

        //predict

        predict.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                try {
                    System.out.println("Inside Predict...");
                    svm_model model = run();
                    System.out.println("Model created");
                    predict_run(model);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

                try {
                    //file = new File("/data/data/" + getPackageName() + "/output.txt");
                    fin = new FileReader("/data/data/" + getPackageName() + "/output.txt");
                    br = new BufferedReader(fin);

                    String line = br.readLine();

                    while(line != null)
                    {
                        line = br.readLine();
                        break;
                    }

                    String[] items = line.split(" ");
                    if(items[0].equals("1.0"))
                        classifier.setText("Eating");
                    else if(items[0].equals("2.0"))
                        classifier.setText("Walking");
                    else if(items[0].equals("3.0"))
                        classifier.setText("Running");

                    accuracy_text.setText(accuracy);

                    svm_params = "svm_type: c_svc, " +
                            "kernel_type: rbf, " +
                            "gamma: 5.0E-4, " +
                            "degree: 2 " +
                            "C: 100" +
                            "coef0: 0 " +
                            "mu: 0.5 " +
                            "cache_size: 100 " +
                            "eps: 1E-3 " +
                            "p: 0.1 " +
                            "probability: 0 " +
                            "shrinking: 1 " +
                            "nr_weight: 0 " +
                            "cross_validation: 4-fold";
                    svm_parameters.setText(svm_params);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    //model and predict


    private static double atof(String s)
    {
        double d = Double.valueOf(s).doubleValue();
        if (Double.isNaN(d) || Double.isInfinite(d))
        {
            System.err.print("NaN or Infinity in input\n");
            System.exit(1);
        }
        return(d);
    }

    private static int atoi(String s)
    {
        try
        {
            return Integer.parseInt(s);
        }
        catch(Exception e)
        {
            return 0;
        }
    }
    private static svm_print_interface svm_print_null = new svm_print_interface()
    {
        public void print(String s) {}
    };
    public svm_model run() throws IOException
    {
        System.out.println("Inside SVM Run..");
        StringBuilder sb=parse_command_line();

        read_problem();
        error_msg = svm.svm_check_parameter(prob,param);
        double ret=0;
        if(error_msg != null)
        {
            System.err.print("ERROR: "+error_msg+"\n");
            System.exit(1);
        }

        if(cross_validation != 0)
        {
            ret = do_cross_validation();
            accuracy = String.valueOf(ret);
            //System.out.println(ret);
        }
        //else
        model = svm.svm_train(prob,param);

        svm.svm_save_model(model_file_name,model);
        // }
        //sb.append(","+ret);
        //return sb.toString();
        return model;
    }

    private StringBuilder parse_command_line()
    {
        int i;
        svm_print_interface print_func = null;	// default printing to stdout
        StringBuilder sb=new StringBuilder();
        param = new svm_parameter();
        // default values
        param.svm_type = svm_parameter.C_SVC;
        param.kernel_type = svm_parameter.RBF;
        param.degree = 2;
        param.gamma = 0.0005;	// 1/num_features
        param.coef0 = 0;
        param.nu = 0.5;
        param.cache_size = 100;
        param.C = 100;
        param.eps = 1e-3;
        param.p = 0.1;
        param.shrinking = 1;
        param.probability = 0;
        param.nr_weight = 0;
        param.weight_label = new int[0];
        param.weight = new double[0];
        cross_validation = 1;
        nr_fold=4;
        sb.append(param.svm_type+",").append(param.kernel_type+",").append(param.degree+",")
                .append(param.gamma+",").append(param.coef0+",").append(param.nu+",").append(param.cache_size+",")
                .append(param.C+",").append(param.eps+",").append(param.p+",").append(param.shrinking+",")
                .append(param.probability+",").append(param.nr_weight+",").append(param.weight_label+",")
                .append(param.weight+",").append(cross_validation+",").append(nr_fold);

        svm.svm_set_print_string_function(print_func);



        input_file_name = "/data/data/" + packagename + "/training_data.txt";
        model_file_name = "/data/data/" + packagename + "/model.txt";
        return sb;
    }


    private double do_cross_validation()
    {
        int i;
        int total_correct = 0;
        double total_error = 0;
        double sumv = 0, sumy = 0, sumvv = 0, sumyy = 0, sumvy = 0;
        double[] target = new double[prob.l];

        svm.svm_cross_validation(prob,param,nr_fold,target);
        if(param.svm_type == svm_parameter.EPSILON_SVR ||
                param.svm_type == svm_parameter.NU_SVR)
        {
            for(i=0;i<prob.l;i++)
            {
                double y = prob.y[i];
                double v = target[i];
                total_error += (v-y)*(v-y);
                sumv += v;
                sumy += y;
                sumvv += v*v;
                sumyy += y*y;
                sumvy += v*y;
            }
            System.out.print("Cross Validation Mean squared error = "+total_error/prob.l+"\n");
            System.out.print("Cross Validation Squared correlation coefficient = "+
                    ((prob.l*sumvy-sumv*sumy)*(prob.l*sumvy-sumv*sumy))/
                            ((prob.l*sumvv-sumv*sumv)*(prob.l*sumyy-sumy*sumy))+"\n"
            );
        }
        else
        {
            for(i=0;i<prob.l;i++)
                if(target[i] == prob.y[i])
                    ++total_correct;
            System.out.print("Cross Validation Accuracy = "+100.0*total_correct/prob.l+"%\n");
        }
        return 100.0*total_correct/prob.l;
    }
    private void read_problem() throws IOException
    {
        System.out.println("Inside Read Prob...");
        BufferedReader fp = new BufferedReader(new FileReader(input_file_name ));
        Vector<Double> vy = new Vector<Double>();
        Vector<svm_node[]> vx = new Vector<svm_node[]>();
        int max_index = 0;

        while(true)
        {
            String line = fp.readLine();
            if(line == null) break;

            StringTokenizer st = new StringTokenizer(line," \t\n\r\f:");

            vy.addElement(atof(st.nextToken()));
            int m = st.countTokens()/2;
            svm_node[] x = new svm_node[m];
            for(int j=0;j<m;j++)
            {
                x[j] = new svm_node();
                x[j].index = atoi(st.nextToken());
                x[j].value = atof(st.nextToken());
            }
            if(m>0) max_index = Math.max(max_index, x[m-1].index);
            vx.addElement(x);
        }

        prob = new svm_problem();
        prob.l = vy.size();
        prob.x = new svm_node[prob.l][];
        for(int i=0;i<prob.l;i++)
            prob.x[i] = vx.elementAt(i);
        prob.y = new double[prob.l];
        for(int i=0;i<prob.l;i++)
            prob.y[i] = vy.elementAt(i);

        if(param.gamma == 0 && max_index > 0)
            param.gamma = 1.0/max_index;

        if(param.kernel_type == svm_parameter.PRECOMPUTED)
            for(int i=0;i<prob.l;i++)
            {
                if (prob.x[i][0].index != 0)
                {
                    System.err.print("Wrong kernel matrix: first column must be 0:sample_serial_number\n");
                    System.exit(1);
                }
                if ((int)prob.x[i][0].value <= 0 || (int)prob.x[i][0].value > max_index)
                {
                    System.err.print("Wrong input format: sample_serial_number out of range\n");
                    System.exit(1);
                }
            }

        fp.close();
    }


    //Predict-
    private static void predict(BufferedReader input, DataOutputStream output, svm_model model, int predict_probability) throws IOException
    {
        int correct = 0;
        int total = 0;
        double error = 0;
        double sumv = 0, sumy = 0, sumvv = 0, sumyy = 0, sumvy = 0;

        int svm_type=svm.svm_get_svm_type(model);
        int nr_class=svm.svm_get_nr_class(model);
        double[] prob_estimates=null;

        if(predict_probability == 1)
        {
            if(svm_type == svm_parameter.EPSILON_SVR ||
                    svm_type == svm_parameter.NU_SVR)
            {
                //svm.svm_predict().info("Prob. model for test data: target value = predicted value + z,\nz: Laplace distribution e^(-|z|/sigma)/(2sigma),sigma="+svm.svm_get_svr_probability(model)+"\n");
            }
            else
            {
                int[] labels=new int[nr_class];
                svm.svm_get_labels(model,labels);
                prob_estimates = new double[nr_class];
                output.writeBytes("labels");
                for(int j=0;j<nr_class;j++)
                    output.writeBytes(" "+labels[j]);
                output.writeBytes("\n");
            }
        }
        while(true)
        {
            String line = input.readLine();
            if(line == null) break;

            StringTokenizer st = new StringTokenizer(line," \t\n\r\f:");

            double target = atof(st.nextToken());
            int m = st.countTokens()/2;
            svm_node[] x = new svm_node[m];
            for(int j=0;j<m;j++)
            {
                x[j] = new svm_node();
                x[j].index = atoi(st.nextToken());
                x[j].value = atof(st.nextToken());
            }

            double v;
            if (predict_probability==1 && (svm_type==svm_parameter.C_SVC || svm_type==svm_parameter.NU_SVC))
            {
                v = svm.svm_predict_probability(model,x,prob_estimates);
                output.writeBytes(v+" ");
                for(int j=0;j<nr_class;j++)
                    output.writeBytes(prob_estimates[j]+" ");
                output.writeBytes("\n");
            }
            else
            {
                v = svm.svm_predict(model,x);
                output.writeBytes(v+"\n");
            }

            if(v == target)
                ++correct;
            error += (v-target)*(v-target);
            sumv += v;
            sumy += target;
            sumvv += v*v;
            sumyy += target*target;
            sumvy += v*target;
            ++total;
        }
        /*if(svm_type == svm_parameter.EPSILON_SVR ||
                svm_type == svm_parameter.NU_SVR)
        {
            // svm_predict.info("Mean squared error = "+error/total+" (regression)\n");
            // svm_predict.info("Squared correlation coefficient = "+
            // ((total*sumvy-sumv*sumy)*(total*sumvy-sumv*sumy))/
            //       ((total*sumvv-sumv*sumv)*(total*sumyy-sumy*sumy))+
            //" (regression)\n");
        }
        else
            System.out.println("Accuracy = "+(double)correct/total*100+
                    "% ("+correct+"/"+total+") (classification)\n");*/
    }

    private static void exit_with_help()
    {
        System.err.print("usage: svm_predict [options] test_file model_file output_file\n"
                +"options:\n"
                +"-b probability_estimates: whether to predict probability estimates, 0 or 1 (default 0); one-class SVM not supported yet\n"
                +"-q : quiet mode (no outputs)\n");
        System.exit(1);
    }

    private void predict_run(svm_model model1) throws IOException
    {
        int i, predict_probability=1;
        //svm_print_string = svm_print_stdout;
        System.out.println("Inside Predict Run.. " + packagename);
        String test_data = "/data/data/" + packagename + "/test_data.txt";
        String out_data ="/data/data/" + packagename + "/output.txt";
        try
        {
            BufferedReader input = new BufferedReader(new FileReader(test_data));

            System.out.println("test data read");
            DataOutputStream output = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(out_data)));
            System.out.println("output file read");
            svm_model model = svm.svm_load_model(model_file_name);
            if (model == null)
            {
                System.err.print("can't open model file ");
                System.exit(1);
            }
            else
            {
                System.out.println("model loaded");
            }
		/*	if(predict_probability == 1)
			{
				if(svm.svm_check_probability_model(model)==0)
				{
					System.err.print("Model does not support probabiliy estimates\n");
					System.exit(1);
				}
			}
			else
			{
				if(svm.svm_check_probability_model(model)!=0)
				{
					svm_predict.info("Model supports probability estimates, but disabled in prediction.\n");
				}
			}*/
            System.out.println("before predict");
            predict(input,output,model,predict_probability);
            input.close();
            output.close();
        }
        catch(FileNotFoundException e)
        {
            exit_with_help();
        }
        catch(ArrayIndexOutOfBoundsException e)
        {
            exit_with_help();
        }
    }
}
