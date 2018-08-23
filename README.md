# aws-redirect-manager

> A CLI tool to manage Route 53 web redirections

`aws-redirect-manager` is a tool that makes managing web redirections with AWS
Route 53 easy.

Traditional DNS providers usually offer a way to setup web redirections
(i.e: test.example.com redirects to https://example.com/test). With AWS
Route 53, setting up a redirection is a bit trickier. You need to:

* Create an empty S3 Bucket with the name of the domain.
* Set up static website hosting on the bucket, and make it redirect to
the desired location.
* Create an `A` record in the Route 53 domain hosted zone that is aliased
to S3.

This is easy enough, but automating it can get tedious. In our case, we had
a lot of subdomain redirections that broke when migrating our legacy app to
AWS. This tool makes maintaining the redirections easy.

## How to use

This tool is only built for UNIX systems only. You will need to compile it
manually on Windows.

You need to have Java installed, and `java` in your PATH.

First you will need to install and configure the
[AWS CLI](https://aws.amazon.com/cli/). You will also need to go to the
[IAM Console](https://console.aws.amazon.com/iam/home#/home) and create an
IAM user for your CLI. Last, you will need to create a policy for your
user to control Route 53 and S3.

Go to `Policies`, click on `Create policy`, click on `JSON` and paste the
following JSON code:

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Sid": "AwsRedirectManager",
            "Effect": "Allow",
            "Action": [
                "route53:ListHostedZones",
                "route53:ChangeResourceRecordSets",
                "route53:ListResourceRecordSets",
                "s3:GetBucketWebsite",
                "s3:PutBucketWebsite",
                "s3:PutBucketAcl",
                "s3:CreateBucket",
                "s3:ListBucket",
                "s3:HeadBucket",
                "s3:DeleteBucket"
            ],
            "Resource": "*"
        }
    ]
}
```

This will give `aws-redirect-manager` enough permission to do its job.
Click on `Review policy`, give it a name, then save it. Once you're
done, attach it to the user you've created. Then you can configure the
AWS CLI, if you haven't before:

```
$ aws configure
AWS Access Key ID [None]: ********************
AWS Secret Access Key [None]: ****************************************
Default region name [None]: eu-west-1
Default output format [None]: json
```

This will save your credentials to `~/.aws/credentials`, and
`aws-redirect-manager` will be able to retrieve them.

Then, in order to run the tool, just download it and execute it:

```sh
$ wget https://github.com/FACUA/aws-redirect-manager/releases/download/v1.1/aws-redirect-manager
$ chmod +x ./aws-redirect-manager
$ ./aws-redirect-manager

aws-redirect-manager v1.1
A tool to manage Route 53 web redirections.

Usage: aws-redirect-manager [command]

Available commands are:

   list        Lists all existing redirections
   create      Creates a new redirection
   bulk-create Creates redirections in bulk
   delete      Deletes an existing redirection

Run a command with --help to get further help.
```

## How to build

You may build the executable by using [Docker](https://www.docker.com/):

```bash
git clone https://github.com/FACUA/aws-redirect-manager
cd aws-redirect-manager
mkdir out
docker build -t aws-redirect-manager .
docker run -v "$(pwd)/out":/out aws-redirect-manager
```