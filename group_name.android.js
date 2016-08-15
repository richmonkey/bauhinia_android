/**
 * Sample React Native App
 * https://github.com/facebook/react-native
 * @flow
 */

import React, { Component } from 'react';
import {
  AppRegistry,
  StyleSheet,
  Text,
  Image,
  ScrollView,
  TouchableHighlight,
  TextInput,
  View
} from 'react-native';


import NavigationBar from 'react-native-navbar';
import { NativeModules, NativeAppEventEmitter } from 'react-native';
import Spinner from 'react-native-loading-spinner-overlay';


class GroupName extends Component {
  constructor(props) {
    super(props);
    this.state = {topic:this.props.topic, visible:false};
  }

  componentDidMount() {

  }

  updateName() {
    if (this.state.topic == this.props.topic) {
      return;
    }
    console.log("update group name...");

    var name = this.state.topic;
    var url = this.props.url + "/groups/" + this.props.group_id;

    this.setState({visible:true});

    fetch(url, {
      method:"PATCH",
      headers: {
        'Accept': 'application/json',
        'Content-Type': 'application/json',
        "Authorization": "Bearer " + this.props.token,
      },
      body:JSON.stringify({name:name}),
    }).then((response) => {
      console.log("status:", response.status);
      if (response.status == 200) {
        this.setState({visible:false});
        this.props.eventEmitter.emit("name_updated", {name:name});
        this.props.navigator.pop();
      } else {
        return response.json().then((responseJson)=>{
          this.setState({visible:false});
          console.log(responseJson.meta.message);
          ToastAndroid.show(responseJson.meta.message, ToastAndroid.LONG);
        });
      }
    }).catch((error) => {
      console.log("error:", error);
      this.setState({visible:false});
      ToastAndroid.show('' + error, ToastAndroid.LONG);
    });

  }

  componentWillUnmount() {

  }

  render() {
    console.log("render group name");

    var self = this;
    var leftButtonConfig = {
      title: '取消',
      handler: () => {
        self.props.navigator.pop();
      }
    };

    var rightButtonConfig = {
      title: '确定',
      handler: () => {
        self.updateName();
      }
    };
    var titleConfig = {
      title: '群聊名称',
    };



    return (
      <View style={{flex:1}}>
        <NavigationBar
            statusBar={{hidden:true}}
            style={{}}
            title={titleConfig}
            leftButton={leftButtonConfig} 
            rightButton={rightButtonConfig} />

        <ScrollView style={{flex:1, backgroundColor:"#F5FCFF"}}>
          <View style={{marginTop:12}}>
            <Text style={{marginLeft:12, marginBottom:4}}>群聊名称</Text>
            <TextInput
                style={{paddingLeft:12, height: 40, backgroundColor:"white"}}
                placeholder=""
                onChangeText={(text) => this.setState({topic:text})}
                value={this.state.topic}/>
          </View>
        </ScrollView>

        <Spinner visible={this.state.visible} />
      </View>
    );
  }
  
}

const styles = StyleSheet.create({
  
});

module.exports = GroupName;
