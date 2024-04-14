package helloworld;

import java.util.HashMap;
import java.util.Map;
import java.util.List;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;


public class App implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard().build();
    private static final DynamoDBMapper mapper = new DynamoDBMapper(client);
    public APIGatewayProxyResponseEvent handleRequest(final APIGatewayProxyRequestEvent input, final Context context) {
        String httoMethod = input.getHttpMethod();
        String output;
        int statusCode = switch (input.getHttpMethod()) {
            case "GET" -> {
                output = this.getUsers();
                yield 200;
            }
            case "POST" -> {
                output = this.createUser(input.getBody());
                yield 201;
            }
            case "PUT" -> {
                output = this.updateUser(input.getBody());
                yield 200;
            }
            case "DELETE" -> {
                output = this.deleteUser(input.getBody());
                yield 200;
            }
            default -> {
                output = "Invalid HTTP Method";
                yield 400;
            }
        };

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("X-Custom-Header", "application/json");

        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent()
                .withStatusCode(statusCode)
                .withHeaders(headers)
                .withBody(output);
        return response;

    }

    private String getUsers() {
        List<User> users = mapper.scan(User.class, new DynamoDBScanExpression());
        return (new Gson()).toJson(users);
    }

    private String createUser(String requestBody) {
        User user = (User) (new Gson()).fromJson(requestBody, User.class);
        mapper.save(user);
        return "User created" + user.getId();
    }

    private String updateUser(String requestBody) {
        User updateUser = (User) (new Gson()).fromJson(requestBody, User.class);
        User existingUser = (User) mapper.load(User.class, updateUser.getId());
        if (existingUser != null) {
            existingUser.setEmpId(updateUser.getEmpId());
            existingUser.setName(updateUser.getName());
            existingUser.setEmail(updateUser.getEmail());
            mapper.save(existingUser);
            return "User updated" + existingUser.getId();
        } else {
            return "User not found";
        }
    }

    private String deleteUser(String requestBody) {
        User deleteUser = (User) (new Gson()).fromJson(requestBody, User.class);
        User existingUser = (User) mapper.load(User.class, deleteUser.getId());
        if (existingUser != null) {
            mapper.delete(existingUser);
            return "User deleted" + existingUser.getId();
        } else {
            return "User not found";
        }
    }

}

