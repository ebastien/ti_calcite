require 'bundler/setup'
require 'pathname'
require 'yaml'
require 'erb'

def ensure_link(target, link)
  ln_sf(target, link) unless File.exists? link
end

def shellout(cmd, chdir = Rails.root)
  $stderr.puts "#{chdir}$ #{cmd}"
  Dir.chdir(chdir) do
    Bundler.with_clean_env do
      system cmd
      fail "Failed to run #{cmd}" unless $?.success?
    end
  end
end

app_path = File.dirname(__FILE__)

config_file = File.join app_path, 'release.yml'

config = begin
  YAML.load_file config_file
rescue Exception => e
  raise ArgumentError, "Could not load release configuration file: #{e}"
end

app_name = config['name'] || 'ti-app'
app_version = config['version'] || '0.1.0'
app_iteration= config['iteration'] || '1'
app_arch = config['arch'] || 'all'
app_description = config['description'] || 'A TI application.'
app_dependencies = config['dependencies'] || []
app_hooks = config['hooks'] || []
app_config_files = config['config_files'] || []

def cmd_service_disable(service)
  "/bin/systemctl disable #{service}.service"
end

def cmd_service_setup(service)
  "/bin/systemctl daemon-reload"
end

def cmd_service_start(service)
  "/bin/systemctl start #{service}.service"
end

def cmd_service_stop(service)
  "/bin/systemctl stop #{service}.service"
end

def cmd_service_force_stop(service)
  "/bin/systemctl stop #{service}.service"
end

resources = [
  'release.yml',
  'build/', 'build/libs/', "build/libs/#{app_name}-all.jar"
]

app_user = "www-data"
app_group = "www-data"

lib_path = "/var/lib"
log_path = "/var/log"
run_path = "/var/run"
etc_path = "/etc"
sv_path = "/etc/sv"

app_lib_path = File.join lib_path, app_name
app_log_path = File.join log_path, app_name
app_run_path = File.join run_path, app_name
app_etc_path = File.join etc_path, app_name

app_service = app_name

app_sv_path = File.join sv_path, app_service

config_files = app_config_files | [
]

directories_runit = [app_sv_path]

directories = [
  app_lib_path,
  app_log_path,
  app_run_path,
  app_etc_path
]

templates_systemd = [
  ['systemd-server-service.erb',
    File.join(app_lib_path, "#{app_service}.service"), 0644]
]

templates = [
  ['postinst.erb', 'postinst'],
  ['prerm.erb', 'prerm']
] + templates_systemd

app_hooks.each do |hook|
  parts_path = File.join "/etc", hook
  postinst_path = File.join parts_path, "postinst.d", app_name
  prerm_path = File.join parts_path, "prerm.d", app_name
  templates += [
    ['hook-postinst.erb', postinst_path],
    ['hook-prerm.erb', prerm_path]
  ]
  directories << parts_path
end

build_path = File.join app_path, 'tmp/build'

package_name = "#{app_name}_#{app_version}-#{app_iteration}_#{app_arch}.deb"
package_path = File.join build_path, package_name

directory build_path

namespace :ti_calcite do

  desc "Build the distribution files of #{app_name}."
  task :distribution => build_path do

    directories.each do |d|
      dist_dir = File.join build_path, d
      mkdir_p dist_dir
    end

    # Realize ERB templates
    templates.each do |template, dest, mode|
      source = File.join app_path, 'tasks/debian', template
      target = File.join build_path, dest
      mkdir_p File.dirname target
      $stderr.puts "Realize template #{source} as #{target}"
      erb = ERB.new(File.read(source))
      erb.filename = File.basename source
      File.open(target, 'w') { |f| f.write erb.result(binding) }
      chmod (mode || 0755), target
    end

    app_pathname = Pathname.new app_path

    resources.each do |resource|
      dirs = []
      files = []

      FileList[File.join app_path, resource].each do |source|
        if File.exist? source
          relsrc = Pathname.new(source).relative_path_from app_pathname
          case File.ftype source
          when 'directory'
            dirs << relsrc
          when 'file'
            files << relsrc
          end
        end
      end

      dirs.each do |d|
        target = File.join build_path, app_lib_path, d
        mkdir_p target
      end

      files.each do |f|
        source = File.join app_path, f
        target = File.join build_path, app_lib_path, f
        cp source, target
      end
    end

    config_files.each do |cfg|
      sample = File.join(app_path, "config", cfg + ".sample")
      # Do not ship the development configuration
      rm_f File.join(build_path, app_lib_path, "config", cfg)
      # Copy the sample configuration to the config path
      cp sample, File.join(build_path, app_etc_path, cfg) if File.exist? sample
    end
  end

  desc "Package the application as #{package_name}."
  task :package => 'ti_calcite:distribution' do

    fpm_bin = "bundle exec fpm"

    # Compute relative paths without trailing './'
    root_pathname = Pathname.new('/')
    rel_dirs = directories.map do |d|
      Pathname.new(d).relative_path_from(root_pathname).to_s
    end
    rel_cfg_files = config_files.map do |f|
      if File.exist? File.join(build_path, app_etc_path, f)
        Pathname.new(File.join app_etc_path, f)
                .relative_path_from(root_pathname).to_s
      end
    end.compact

    # Debian "maintainer scripts"
    postinst_script = File.join build_path, 'postinst'
    prerm_script = File.join build_path, 'prerm'

    cfg_flags = rel_cfg_files.map { |f| "--config-files #{f}" }.join ' '
    dep_flags = app_dependencies.map { |d| "-d \"#{d}\"" }.join ' '

    fpm_cmd = "#{fpm_bin} -p #{package_name} -n #{app_name} -v #{app_version}" \
              " --iteration #{app_iteration} -a #{app_arch}" \
              " --deb-user #{app_user} --deb-group #{app_group}" \
              " --after-install #{postinst_script}" \
              " --before-remove #{prerm_script}" \
              " #{dep_flags}" \
              " #{cfg_flags}" \
              " --description \"#{app_description}\"" \
              " --exclude \"*/vendor/cache/*\"" \
              " -t deb -s dir #{rel_dirs.join ' '}"

    rm_f package_path

    shellout fpm_cmd, build_path

    manifest_file = File.join build_path, "manifest"
    open(manifest_file, 'w') do |f|
      f.write package_name
    end
  end
end

