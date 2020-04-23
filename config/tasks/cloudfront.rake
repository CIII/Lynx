# Tasks used to upload new items to s3 and then invalidate cache in cloudfront

require 'aws-sdk'
require 'aws-sdk-core'
require 'yaml'
require 'logger'
require 'simple-cloudfront-invalidator'
require 'mime-types'
require 'digest'
require 'fileutils'
require "yui/compressor"

log = Logger.new(STDOUT)
log.level = Logger::DEBUG

config = YAML.load_file("config/tasks/cloudfront_config.yml")

pwd = Dir.getwd

Aws.config[:ssl_ca_bundle] = "#{Gem.loaded_specs['aws-sdk-core'].full_gem_path}/ca-bundle.crt"

namespace :aws do

  # Upload to s3
  task :s3_upload, :files do |t, args|

    puts ('Upload files to s3?[Yes/No]')
    response = STDIN.gets

    if response.chomp.downcase == 'yes'

      puts 'Uploading contents to s3 bucket ---->'
      unless fetch(:stage) == :prod || fetch(:stage) == :staging
        puts "Only for production"
        next
      end

      s3 = Aws::S3::Resource.new(region: 'us-west-2')

      bucket = s3.bucket(config[:deploy][:s3_bucket][fetch(:stage)])

      Dir.chdir(config[:deploy][:from_folder])

      to_upload ={}
      to_minify_and_upload = {}
      unsuccessful = []

      unless (files = args[:files])
        regex = "**/*"
      else
        regex = files
      end

      Dir[regex].each do |file|

        next if File.directory?(file)
        log.debug("Processing file #{file}")
        if(file[-5..-1] == "woff2") then
          mime_type = MIME::Type.new("application/font-woff2")
        elsif (file[-6..-1] == "js.map" || file[-7..-1] == "min.map") then
          mime_type = MIME::Type.new("application/js")
        elsif (file[-7..-1] == "css.map") then
          mime_type = MIME::Type.new("text/css")
        else
          mime_type = MIME::Types.type_for(file).first
        end

        unless mime_type.nil?
          mime_type = mime_type.simplified
          to_upload[file] = mime_type
        else
          log.info("Could not upload #{file}, file does not have a mime type")
          unsuccessful.push file
          next
        end
      end

      jsCompressor = YUI::JavaScriptCompressor.new
      cssCompressor = YUI::CssCompressor.new

      client = s3.client
      md5 = Digest::MD5.new
      to_upload.each do |file,mime_type|
        md5.reset
        begin
          obj = client.head_object({'bucket': bucket.name, 'key': file })
          etag = obj.etag.gsub('"', '')
        rescue StandardError => e
          puts("New file found " + file)
          etag = ''
        end # requires etag != null
        file_ext = File.extname(file)
        if (file_ext == '.js' || file_ext == '.css') && (!file.include? 'bower_components')
          fileContent = File.open(file, "r").read
          minified = false
          begin
            if file_ext == '.js'
              fileContent = jsCompressor.compress(fileContent)
            elsif file_ext == '.css'
              fileContent = cssCompressor.compress(fileContent)
            end
            minified = true
          rescue
            puts("Unable to minify " + file)
            minified = false
          end

          if(minified)
            to_upload.delete(file)
            digest = md5.hexdigest(fileContent)
            if(etag != digest)
              puts("Change detected: " + file + "(" + etag + "/" + digest + ")")
              newFile = 'tmp/minify/' + file
              FileUtils.mkdir_p(File.dirname(newFile))
              File.open(newFile, 'w') {|f| f.write(fileContent) }
              to_minify_and_upload[file] = {'new_file': newFile, 'mime_type': mime_type}
            end
          else
            digest = md5.hexdigest(File.read(file))
            if(etag == digest)
              to_upload.delete(file)
            else
              puts("Change detected: " + file + "(" + etag + "/" + digest + ")")
            end
          end
        else
          digest = md5.hexdigest(File.read(file))
          if(etag == digest)
            to_upload.delete(file)
          else
            puts("Change detected: " + file + "(" + etag + "/" + digest + ")")
          end
        end
      end

      puts('Upload the following files?')
      to_upload.keys.each do |file|
        puts "\t#{file}"
      end
      to_minify_and_upload.keys.each do |file|
        puts "\t#{file}"
      end

      puts ('Upload the above files?[Yes/No]')
      response = STDIN.gets

      set :toUpload, to_upload
      set :toMinifyAndUpload, to_minify_and_upload

      if response.chomp.downcase == 'yes'

        to_upload.each do |file,mime_type|
          log.debug("Uploading #{file} with Content-Type: #{mime_type}")
          headers = {
            'acl': 'public-read',
            'content_type': mime_type,
            'cache_control': "max-age=" + (1 * 7 * 24 * 60 * 60).to_s, # One week, calculated in seconds
          }
          obj = bucket.object(file)
          obj.upload_file(file, headers)
        end

        to_minify_and_upload.each do |file, new_file|
          log.debug("Uploading #{file} with Content-Type: #{new_file[:mime_type]}")
          headers = {
          'acl': 'public-read',
          'content_type': new_file[:mime_type]
          }
          obj = bucket.object(file)
          obj.upload_file(new_file[:new_file], headers)
        end

        FileUtils.remove_dir('tmp/minify/') if File.directory?('tmp/minify')

        log.info("Done!")
        puts 'WARNING: Files that were unable to be uploaded --->' if unsuccessful.size > 0
        unsuccessful.each do |file|
          puts "\t#{file}"
        end
      else
        puts 'Files not uploaded'
      end

      Dir.chdir(pwd)
    else
      to_upload ={}
      to_minify_and_upload = {}
      set :toUpload, to_upload
      set :toMinifyAndUpload, to_minify_and_upload
    end
  end
 
  # Invalidate Cloudfront distribution
  task :invalidate, :files do |t, args| 
 
    client = Aws::CloudFront::Client.new(region: 'us-west-2')

    puts 'Invalidating contents in cloudfront ---->'
    unless fetch(:stage) == :prod || fetch(:stage) == :staging
      puts "Only for production"
      next
    end
 
    Dir.chdir(config[:deploy][:from_folder])
 
    unless (files = args[:files])
      regex = "**/*"
    else
      regex = files
    end

    log.info("Beginning invalidation request.")
    
    files_to_invalidate =[]

    to_upload = fetch(:toUpload, nil)
    to_minify_and_upload = fetch(:toMinifyAndUpload, nil)

    if to_upload.nil? && to_minify_and_upload.nil?
      Dir[regex].each do |file|
        begin
          next if File.directory?(file)
          files_to_invalidate.push (URI::encode(file.sub! './', ''))
        rescue Exception => e
          puts "Error #{e}"
        end
      end
    else
      to_upload.keys.each do |file|
        begin
      	  files_to_invalidate.push (URI::encode("/" + (file.sub './', '')))
        rescue Exception => e
          puts "Error #{e}"
        end
      end
      to_minify_and_upload.keys.each do |file|
        begin
          files_to_invalidate.push (URI::encode("/" + (file.sub './', '')))
        rescue Exception => e
          puts "Error #{e}"
        end
      end
    end
    files_to_invalidate.push("/easiersolar/static/*")
    files_to_invalidate.push("/homesolar/static/*")
    log.debug("Invalidating the following files:\n" + files_to_invalidate.join("\n"))
 
    now = Time.now
    caller_reference = now.year.to_s + now.month.to_s + now.day.to_s + now.hour.to_s + now.min.to_s
 	descriptor = {
    	distribution_id: config[:deploy][:cloudfront][fetch(:stage)],
    	invalidation_batch: {
    		paths: {
    			quantity: files_to_invalidate.length,
    			items: files_to_invalidate
    			},
	    	caller_reference: caller_reference
    		}
    	}
    begin
      puts client.create_invalidation(descriptor)
    rescue Exception => e
      puts "Error Invalidating #{e}"
    end
    log.info("Done!")
 
    Dir.chdir(pwd)
  end
 
  desc "Deploy files to S3 and invalidate Cloudfront distribution"
  task :deploy => [:s3_upload, :invalidate]
end

  task :aws => 'aws:deploy'
