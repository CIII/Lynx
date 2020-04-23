namespace :custom_deploy do

  def generate_val
    app_dir = "#{fetch(:application)}-#{fetch(:app_version_number)}"
    app_dir_path = "#{deploy_to}#{app_dir}"
    zip_file = "#{app_dir}.zip"
    [app_dir, app_dir_path, zip_file]
  end

  task :package do
    dirname = "./target/universal_past/"
    FileUtils.mkdir_p(dirname) unless File.directory?(dirname)
    time = Time.now.to_i
    system "cp -r --backup --suffix=.#{time} ./target/universal/* #{dirname}"
    system "rm -rf ./target/universal/*"
    system "./bin/activator dist"
  end

  task :distribute do
    app_dir, app_dir_path, zip_file = generate_val
    on roles(:all) do |server|
      puts "Distributing standalone play zip file to #{server.hostname}"
      upload! "./target/universal/#{zip_file}", "/home/ubuntu/#{zip_file}"
      execute "unzip -qo #{app_dir}.zip"
      execute "chmod +x ~/#{app_dir}/bin/#{fetch(:application)}"
    end
  end

  def running_pid_exists?(app_dir_path)
    test("ls #{app_dir_path}/RUNNING_PID 2>/dev/null")
  end

  def process_exists?(pid)
    test("ps aux | grep -v grep | grep #{pid}")
  end

  task :stop do
    app_dir, app_dir_path, zip_file = generate_val
    on roles(:all) do
      if running_pid_exists?(app_dir_path)
        running_id = capture "cat #{app_dir_path}/RUNNING_PID"
        execute "sudo kill #{running_id}" if process_exists?(running_id)
        execute "rm -f #{app_dir_path}/RUNNING_PID"
      end
    end
  end

  task :start do
    app_dir, app_dir_path, zip_file = generate_val
    config_file = case fetch(:stage)
      when :staging
        'staging.conf'
      when :production
        'prod.conf'
    end
    on roles(:all) do
      execute "(nohup #{app_dir_path}/bin/easiersolar -Dconfig.file=#{app_dir_path}/conf/#{config_file}) & sleep 5"
    end

  end

  task :verify do
    app_dir, app_dir_path, zip_file = generate_val
    on roles(:all) do |server|
      if running_pid_exists?(app_dir_path)
        running_id = capture "cat #{app_dir_path}/RUNNING_PID"
        unless process_exists?(running_id)
          puts "Server #{server.hostname} is not running"
          exit
        else
          puts "Server #{server.hostname} is running"
        end
      else
        puts "Server #{server.hostname} is not running"
      end
    end
  end

task :test do
 on roles(:all) do |server|
   puts server.hostname
 end
end
  task :deploy => [:package, :distribute, :stop, :start, :verify]

end

task :custom_deploy => 'custom_deploy:deploy'
