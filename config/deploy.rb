# config valid only for current version of Capistrano
lock '3.6.1'

set :application, 'Lynx'
set :app_version_number, '1.0-SNAPSHOT'

set :scm, :git
set :repo_url, 'git@github.com:CIII/Lynx.git'

set :user, "ubuntu"

set :stages, ["staging"]
set :default_stage, "staging"

# Default branch is :master
# ask :branch, `git rev-parse --abbrev-ref HEAD`.chomp
ask :branch, 'master'

# Default deploy_to directory is /var/www/my_app_name
# set :deploy_to, '/var/www/my_app_name'
set :deploy_to, "/var/cap/#{fetch(:application)}"

# Default value for :scm is :git
# set :scm, :git

# Default value for :format is :airbrussh.
# set :format, :airbrussh

# You can configure the Airbrussh format using :format_options.
# These are the defaults.
# set :format_options, command_output: true, log_file: 'log/capistrano.log', color: :auto, truncate: :auto

# Default value for :pty is false
# set :pty, true
set :pty, true

set :ssh_options, {
  forward_agent: true,
  auth_methods: ["publickey"],
  keys: ["~/.ssh/easiersolar_key.pem"]
}

# Default value for :linked_files is []
# append :linked_files, 'config/database.yml', 'config/secrets.yml'

# Default value for linked_dirs is []
# append :linked_dirs, 'log', 'tmp/pids', 'tmp/cache', 'tmp/sockets', 'public/system'

# Default value for default_env is {}
# set :default_env, { path: "/opt/ruby/bin:$PATH" }

# Default value for keep_releases is 5
# set :keep_releases, 5

namespace :deploy do 
  task :compile do
    system "grunt dev"
    invoke "aws:deploy"
    on roles(:easiersolar1) do
      execute "cd #{current_path}/public && bower install"
      execute "cd #{ current_path } && sbt dist -mem 512 -Dconfig.file=conf/#{fetch(:stage)}.conf"
      execute "rm -Rf /opt/#{ fetch(:application) }/*"
      execute "unzip /var/cap/#{ fetch(:application) }/current/target/universal/#{ fetch(:application) }-#{ fetch(:app_version_number) }.zip -d /opt/easiersolar"
      execute "sudo service lynx restart"
    end
  end
  
  after "deploy:published", "deploy:compile"
end